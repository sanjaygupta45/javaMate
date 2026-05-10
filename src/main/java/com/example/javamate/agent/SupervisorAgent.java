package com.example.javamate.agent;

import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.router.RouteDecision;
import com.example.javamate.agent.tracing.AgentTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SupervisorAgent has two responsibilities:
 *   1. ROUTE       - decide which specialist agent(s) should answer (structured output).
 *   2. SYNTHESIZE  - merge multiple specialist answers into one final reply.
 *
 * It never answers questions itself.
 */
@Component
public class SupervisorAgent {

    private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);
    private static final int MAX_HISTORY_FOR_ROUTING = 6;

    private final ChatClient routerClient;
    private final ChatClient synthesizerClient;
    private final AgentTracing tracing;

    public SupervisorAgent(MistralAiChatModel model, AgentTracing tracing) {
        this.tracing = tracing;
        this.routerClient = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.SUPERVISOR_ROUTER)
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .build();
        this.synthesizerClient = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.SUPERVISOR_SYNTHESIZER)
                .defaultOptions(ChatOptions.builder().temperature(0.3).build())
                .build();
    }

    /** Decide which agents to invoke. */
    public Mono<RouteDecision> route(AgentContext ctx) {
        return Mono.fromCallable(() -> tracing.span(
                "agent.supervisor.route",
                Map.of("agent.name", "SUPERVISOR.ROUTE",
                        "input.chars", String.valueOf(ctx.query().length())),
                () -> {
                    String userMsg = "Recent conversation (most recent last):\n"
                            + formatHistory(ctx.history())
                            + "\n\nNew user query:\n" + ctx.query();
                    RouteDecision decision;
                    try {
                        decision = routerClient.prompt().user(userMsg).call().entity(RouteDecision.class);
                    } catch (Exception e) {
                        log.error("[Supervisor] routing failed, falling back to JAVA_KNOWLEDGE: {}", e.getMessage());
                        decision = null;
                    }
                    if (decision == null || decision.agents() == null || decision.agents().isEmpty()) {
                        tracing.tag("route.fallback", "true");
                        return new RouteDecision(List.of(AgentName.JAVA_KNOWLEDGE),
                                ctx.query(), "fallback: router returned no agents");
                    }
                    List<AgentName> trimmed = decision.agents().stream().distinct().limit(2).toList();
                    String refined = (decision.refinedQuery() == null || decision.refinedQuery().isBlank())
                            ? ctx.query() : decision.refinedQuery();
                    String reason = decision.reason() == null ? "" : decision.reason();
                    tracing.tag("route.agents", trimmed.toString());
                    tracing.tag("route.reason", reason);
                    log.info("[Supervisor] route={} refined='{}' reason='{}'", trimmed, refined, reason);
                    return new RouteDecision(trimmed, refined, reason);
                })).subscribeOn(Schedulers.boundedElastic());
    }

    /** Merge multiple specialist answers (blocking). */
    public Mono<String> synthesize(AgentContext ctx, List<AgentResult> results) {
        return Mono.fromCallable(() -> tracing.span(
                "agent.supervisor.synthesize",
                Map.of("synth.specialists", String.valueOf(results.size())),
                () -> synthesizerClient.prompt()
                        .user(buildSynthesisPrompt(ctx, results))
                        .call().content())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /** Merge multiple specialist answers (streaming). */
    public Flux<String> synthesizeStream(AgentContext ctx, List<AgentResult> results) {
        Map<String, String> tags = Map.of("synth.specialists", String.valueOf(results.size()));
        String userPrompt = buildSynthesisPrompt(ctx, results);
        return tracing.wrap("agent.supervisor.synthesize.stream", tags,
                synthesizerClient.prompt().user(userPrompt).stream().content());
    }

    private String buildSynthesisPrompt(AgentContext ctx, List<AgentResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original user question:\n").append(ctx.query()).append("\n\n");
        sb.append("Specialist agent answers:\n");
        for (AgentResult r : results) {
            sb.append("\n--- ").append(r.agent()).append(" ---\n")
                    .append(r.content()).append("\n");
        }
        sb.append("\nProduce one cohesive final answer for the user.");
        return sb.toString();
    }

    private String formatHistory(List<Message> hist) {
        if (hist == null || hist.isEmpty()) return "(no prior messages)";
        int start = Math.max(0, hist.size() - MAX_HISTORY_FOR_ROUTING);
        return hist.subList(start, hist.size()).stream()
                .map(m -> m.getMessageType() + ": " + truncate(m.getText(), 240))
                .collect(Collectors.joining("\n"));
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
