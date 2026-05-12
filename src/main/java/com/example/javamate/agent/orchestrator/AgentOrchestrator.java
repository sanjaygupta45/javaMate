package com.example.javamate.agent.orchestrator;

import com.example.javamate.agent.Agent;
import com.example.javamate.agent.AgentContext;
import com.example.javamate.agent.AgentName;
import com.example.javamate.agent.AgentResult;
import com.example.javamate.agent.SupervisorAgent;
import com.example.javamate.agent.events.AgentEventBus;
import com.example.javamate.agent.events.AgentStreamEvent;
import com.example.javamate.agent.router.RouteDecision;
import com.example.javamate.agent.tracing.AgentTracing;
import com.example.javamate.dto.OrchestratorResult;
import com.example.javamate.dto.TextQueryResponseDTO.CitationRef;
import com.example.javamate.dto.TextQueryResponseDTO.SourceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final SupervisorAgent supervisor;
    private final Map<AgentName, Agent> agents;
    private final ChatMemory chatMemory;
    private final AgentTracing tracing;
    private final AgentEventBus eventBus;

    public AgentOrchestrator(SupervisorAgent supervisor,
                             List<Agent> agentBeans,
                             ChatMemory chatMemory,
                             AgentTracing tracing,
                             AgentEventBus eventBus) {
        this.supervisor = supervisor;
        this.chatMemory = chatMemory;
        this.tracing = tracing;
        this.eventBus = eventBus;
        this.agents = agentBeans.stream()
                .collect(Collectors.toMap(Agent::name, Function.identity(),
                        (a, b) -> a, () -> new EnumMap<>(AgentName.class)));
        log.info("AgentOrchestrator initialized with agents: {}", this.agents.keySet());
    }

    public Mono<String> handle(String query, Long userId, String sessionId) {
        return handleDetailed(query, userId, sessionId).map(OrchestratorResult::answer);
    }


    public Mono<OrchestratorResult> handleDetailed(String query, Long userId, String sessionId) {
        String convId = conversationId(userId, sessionId);
        Map<String, String> tags = Map.of(
                "user.id", String.valueOf(userId),
                "session.id", sessionId,
                "mode", "non-stream");

        eventBus.register(convId);

        Mono<OrchestratorResult> pipeline = tracing.wrap("agent.orchestrator.handle", tags,
                prepareContext(query, convId, userId, sessionId)
                        .flatMap(ctx -> supervisor.route(ctx)
                                .doOnNext(d -> eventBus.emit(convId, new AgentStreamEvent.RouteEvent(
                                        d.agents(), d.reason(), d.refinedQuery())))
                                .flatMap(decision -> runAgents(ctx, decision)
                                        .flatMap(results -> finalAnswer(ctx, decision, results)
                                                .map(answer -> new RouteAndAnswer(decision, answer))))))
                .flatMap(ra -> persistAssistant(convId, ra.answer())
                        .thenReturn(buildResultFromBus(convId, ra)));

        return pipeline
                .doOnSuccess(r -> {
                    eventBus.emit(convId, new AgentStreamEvent.DoneEvent(
                            sessionId, r.answer() == null ? 0L : approxTokens(r.answer())));
                    eventBus.complete(convId);
                })
                .doOnError(e -> eventBus.complete(convId))
                .doOnCancel(() -> eventBus.complete(convId));
    }

    public Flux<String> handleStream(String query, Long userId, String sessionId) {
        String convId = conversationId(userId, sessionId);
        Map<String, String> tags = Map.of(
                "user.id", String.valueOf(userId),
                "session.id", sessionId,
                "mode", "stream");
        return tracing.wrap("agent.orchestrator.handle.stream", tags,
                prepareContext(query, convId, userId, sessionId)
                        .flatMapMany(ctx -> supervisor.route(ctx)
                                .flatMapMany(decision -> streamAnswer(ctx, decision, convId))));
    }


    public Flux<AgentStreamEvent> handleStreamEvents(String query, Long userId, String sessionId) {
        String convId = conversationId(userId, sessionId);
        Map<String, String> tags = Map.of(
                "user.id", String.valueOf(userId),
                "session.id", sessionId,
                "mode", "stream");

        var sink = eventBus.register(convId);
        AtomicLong tokenCount = new AtomicLong();

        Mono<Void> runner = tracing.wrap("agent.orchestrator.handle.stream", tags,
                prepareContext(query, convId, userId, sessionId)
                        .flatMap(ctx -> supervisor.route(ctx)
                                .doOnNext(d -> eventBus.emit(convId, new AgentStreamEvent.RouteEvent(
                                        d.agents(), d.reason(), d.refinedQuery())))
                                .flatMap(decision -> runStreamingAgents(ctx, decision, convId, tokenCount))));

        // Kick off the runner; events flow through the bus.
        runner
                .doFinally(sig -> {
                    eventBus.emit(convId, new AgentStreamEvent.DoneEvent(sessionId, tokenCount.get()));
                    eventBus.complete(convId);
                })
                .subscribe(null,
                        err -> log.error("[Orchestrator] stream pipeline failed", err));

        return sink.asFlux();
    }

    // ---------------------------------------------------------------
    // Pipeline steps
    // ---------------------------------------------------------------

    private Mono<AgentContext> prepareContext(String query, String convId, Long userId, String sessionId) {
        return Mono.fromCallable(() -> {
                    List<Message> history = chatMemory.get(convId);
                    chatMemory.add(convId, List.of(new UserMessage(query)));
                    return history == null ? List.<Message>of() : history;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(history -> new AgentContext(query, userId, sessionId, history));
    }

    private Mono<List<AgentResult>> runAgents(AgentContext ctx, RouteDecision decision) {
        AgentContext refined = ctx.withQuery(decision.refinedQuery());
        List<Mono<AgentResult>> calls = decision.agents().stream()
                .map(agents::get)
                .filter(Objects::nonNull)
                .map(a -> a.run(refined)
                        .doOnNext(r -> log.debug("[Orchestrator] agent={} produced {} chars",
                                r.agent(), r.content() == null ? 0 : r.content().length())))
                .toList();
        if (calls.isEmpty()) {
            log.warn("[Orchestrator] No valid agents matched route {}", decision.agents());
            return Mono.just(List.of());
        }
        return Flux.merge(calls).collectList();
    }

    private Mono<String> finalAnswer(AgentContext ctx, RouteDecision decision, List<AgentResult> results) {
        if (results.isEmpty()) {
            return Mono.just("I couldn't route your question to any specialist. Please rephrase.");
        }
        if (results.size() == 1) {
            return Mono.just(results.get(0).content());
        }
        return supervisor.synthesize(ctx, results);
    }

    private Flux<String> streamAnswer(AgentContext ctx, RouteDecision decision, String convId) {
        AgentContext refined = ctx.withQuery(decision.refinedQuery());
        List<AgentName> chosen = decision.agents();

        Flux<String> tokens;
        if (chosen.size() == 1) {
            Agent a = agents.get(chosen.get(0));
            if (a == null) {
                tokens = Flux.just("I couldn't route your question. Please rephrase.");
            } else {
                tokens = a.stream(refined);
            }
        } else {
            tokens = Flux.merge(chosen.stream()
                            .map(agents::get).filter(Objects::nonNull)
                            .map(a -> a.run(refined)).toList())
                    .collectList()
                    .flatMapMany(results -> results.isEmpty()
                            ? Flux.just("No specialist could answer.")
                            : supervisor.synthesizeStream(ctx, results));
        }

        StringBuilder buffer = new StringBuilder();
        return tokens
                .doOnNext(buffer::append)
                .doOnComplete(() -> persistAssistant(convId, buffer.toString())
                        .subscribe(null,
                                e -> log.error("[Orchestrator] failed to save assistant message", e)));
    }

    /**
     * Streaming variant that pushes each token onto the event bus as a
     * {@link AgentStreamEvent.TokenEvent} and persists the assistant message
     * at the end. Returns a {@link Mono} that completes when the LLM stream ends.
     */
    private Mono<Void> runStreamingAgents(AgentContext ctx, RouteDecision decision,
                                          String convId, AtomicLong tokenCount) {
        AgentContext refined = ctx.withQuery(decision.refinedQuery());
        List<AgentName> chosen = decision.agents();

        Flux<String> tokens;
        if (chosen.size() == 1) {
            Agent a = agents.get(chosen.get(0));
            tokens = (a == null)
                    ? Flux.just("I couldn't route your question. Please rephrase.")
                    : a.stream(refined);
        } else {
            tokens = Flux.merge(chosen.stream()
                            .map(agents::get).filter(Objects::nonNull)
                            .map(a -> a.run(refined)).toList())
                    .collectList()
                    .flatMapMany(results -> results.isEmpty()
                            ? Flux.just("No specialist could answer.")
                            : supervisor.synthesizeStream(ctx, results));
        }

        StringBuilder buffer = new StringBuilder();
        return tokens
                .doOnNext(t -> {
                    buffer.append(t);
                    tokenCount.incrementAndGet();
                    eventBus.emit(convId, new AgentStreamEvent.TokenEvent(t));
                })
                .then(persistAssistant(convId, buffer.toString()));
    }

    // ---------------------------------------------------------------
    // Memory helpers
    // ---------------------------------------------------------------

    private Mono<Void> persistAssistant(String convId, String answer) {
        if (answer == null || answer.isBlank()) return Mono.empty();
        return Mono.<Void>fromRunnable(() ->
                chatMemory.add(convId, List.of(new AssistantMessage(answer))))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    // ---------------------------------------------------------------
    // Result building (drain the event bus into a DTO-friendly shape)
    // ---------------------------------------------------------------

    /** Drains in-memory bus events to extract sources & citations. */
    private OrchestratorResult buildResultFromBus(String convId, RouteAndAnswer ra) {
        List<SourceRef> sources = new ArrayList<>();
        List<CitationRef> citations = new ArrayList<>();
        List<AgentStreamEvent> events = eventBus.snapshot(convId);
        for (AgentStreamEvent ev : events) {
            if (ev instanceof AgentStreamEvent.ToolEvent te) {
                if ("webSearch".equals(te.name()) && te.results() instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            sources.add(new SourceRef(
                                    asString(m.get("title")),
                                    asString(m.get("url")),
                                    asString(m.get("snippet"))));
                        }
                    }
                } else if ("personal_rag.retrieve".equals(te.name()) && te.results() instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            citations.add(new CitationRef(
                                    asString(m.get("documentId")),
                                    asString(m.get("snippet"))));
                        }
                    }
                }
            }
        }
        return new OrchestratorResult(
                ra.answer(),
                ra.decision().agents(),
                ra.decision().reason(),
                sources,
                citations);
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long approxTokens(String s) {
        // Rough heuristic: ~4 chars per token. Cheap and good enough for telemetry.
        return s == null ? 0L : Math.max(1, s.length() / 4);
    }

    private static String conversationId(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    private record RouteAndAnswer(RouteDecision decision, String answer) {}
}
