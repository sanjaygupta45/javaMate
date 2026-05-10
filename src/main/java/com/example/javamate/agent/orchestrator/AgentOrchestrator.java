package com.example.javamate.agent.orchestrator;

import com.example.javamate.agent.Agent;
import com.example.javamate.agent.AgentContext;
import com.example.javamate.agent.AgentName;
import com.example.javamate.agent.AgentResult;
import com.example.javamate.agent.SupervisorAgent;
import com.example.javamate.agent.router.RouteDecision;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entry point of the multi-agent system.
 *
 * <p>Pipeline per user turn:
 * <pre>
 *   1. Read recent chat history from ChatMemory (for routing context).
 *   2. Persist the raw user message.
 *   3. Ask Supervisor to ROUTE -> RouteDecision (1 or 2 agents).
 *   4. Run selected agents in PARALLEL (blocking calls on boundedElastic).
 *   5. If single agent  -> use its answer directly.
 *      If multiple      -> Supervisor SYNTHESIZES one final answer.
 *   6. Persist the final assistant message (triggers ChatMemory compaction).
 * </pre>
 */
@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final SupervisorAgent supervisor;
    private final Map<AgentName, Agent> agents;
    private final ChatMemory chatMemory;

    public AgentOrchestrator(SupervisorAgent supervisor,
                             List<Agent> agentBeans,
                             ChatMemory chatMemory) {
        this.supervisor = supervisor;
        this.chatMemory = chatMemory;
        this.agents = agentBeans.stream()
                .collect(Collectors.toMap(Agent::name, Function.identity(),
                        (a, b) -> a, () -> new EnumMap<>(AgentName.class)));
        log.info("AgentOrchestrator initialized with agents: {}", this.agents.keySet());
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public Mono<String> handle(String query, Long userId, String sessionId) {
        String convId = conversationId(userId, sessionId);
        return prepareContext(query, convId, userId, sessionId)
                .flatMap(ctx -> supervisor.route(ctx)
                        .flatMap(decision -> runAgents(ctx, decision)
                                .flatMap(results -> finalAnswer(ctx, decision, results))))
                .flatMap(answer -> persistAssistant(convId, answer).thenReturn(answer));
    }

    public Flux<String> handleStream(String query, Long userId, String sessionId) {
        String convId = conversationId(userId, sessionId);
        return prepareContext(query, convId, userId, sessionId)
                .flatMapMany(ctx -> supervisor.route(ctx)
                        .flatMapMany(decision -> streamAnswer(ctx, decision, convId)));
    }

    // ---------------------------------------------------------------
    // Pipeline steps
    // ---------------------------------------------------------------

    /** Read recent history (NOT including current query yet) and persist the new user message. */
    private Mono<AgentContext> prepareContext(String query, String convId, Long userId, String sessionId) {
        return Mono.fromCallable(() -> {
                    List<Message> history = chatMemory.get(convId);
                    chatMemory.add(convId, List.of(new UserMessage(query)));
                    return history == null ? List.<Message>of() : history;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(history -> new AgentContext(query, userId, sessionId, history));
    }

    /** Run the chosen specialist agents in parallel and collect their results. */
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

    /** Non-streaming final answer (single agent -> direct, multi -> synthesize). */
    private Mono<String> finalAnswer(AgentContext ctx, RouteDecision decision, List<AgentResult> results) {
        if (results.isEmpty()) {
            return Mono.just("I couldn't route your question to any specialist. Please rephrase.");
        }
        if (results.size() == 1) {
            return Mono.just(results.get(0).content());
        }
        return supervisor.synthesize(ctx, results);
    }

    /** Streaming final answer. For single-agent route we stream the agent directly. */
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
            // Multiple agents: run them blocking-in-parallel, then stream the synthesis.
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

    private static String conversationId(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }
}
