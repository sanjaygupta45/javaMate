package com.example.javamate.agent;

import com.example.javamate.agent.critic.CriticAgent;
import com.example.javamate.agent.critic.CritiqueResult;
import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tracing.AgentTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * Generates non-trivial code using a reflection (drafter -> critic -> reviser) loop.
 *
 * <p>Algorithm:
 * <pre>
 *   draft = codeGen(query)
 *   for i in 1..MAX_ITER:
 *       critique = critic(query, draft)
 *       if critique.approved -> break
 *       draft = codeGen(query, draft, critique)   // revise
 *   return draft
 * </pre>
 *
 * <p>Self-contained: from the orchestrator's perspective this is just another {@link Agent}.
 */
@Component
public class CodeGenAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CodeGenAgent.class);
    private static final int MAX_REVISIONS = 1; // 1 draft + up to 1 revision

    private final ChatClient chat;
    private final CriticAgent critic;
    private final AgentTracing tracing;

    public CodeGenAgent(MistralAiChatModel model, CriticAgent critic, AgentTracing tracing) {
        this.critic = critic;
        this.tracing = tracing;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.CODE_GEN)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    @Override
    public AgentName name() {
        return AgentName.CODE_GEN;
    }

    @Override
    public Mono<AgentResult> run(AgentContext ctx) {
        return tracing.wrap(
                "agent.code_gen",
                Map.of("agent.name", "CODE_GEN", "agent.input.chars", String.valueOf(ctx.query().length())),
                draft(ctx.query())
                        .flatMap(d -> reflect(ctx.query(), d, 0))
                        .map(finalCode -> new AgentResult(name(), finalCode))
        );
    }

    /** Streaming not supported - reflection requires multiple complete LLM passes. */
    @Override
    public reactor.core.publisher.Flux<String> stream(AgentContext ctx) {
        return run(ctx).flatMapMany(r -> reactor.core.publisher.Flux.just(r.content()));
    }

    // ----- internals -----

    private Mono<String> draft(String userRequest) {
        return Mono.fromCallable(() -> tracing.span(
                "agent.code_gen.draft",
                Map.of("iteration", "0"),
                () -> chat.prompt().user(userRequest).call().content())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> revise(String userRequest, String previousDraft, CritiqueResult critique, int iteration) {
        String revisionPrompt = """
                Original user request:
                %s

                Your previous draft:
                %s

                Critic feedback:
                Issues:
                %s

                Suggestions:
                %s

                Produce an improved version that addresses every issue above.
                """.formatted(
                userRequest,
                previousDraft,
                bullets(critique.issues()),
                critique.suggestions() == null ? "" : critique.suggestions());
        return Mono.fromCallable(() -> tracing.span(
                "agent.code_gen.revise",
                Map.of("iteration", String.valueOf(iteration)),
                () -> chat.prompt().user(revisionPrompt).call().content())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /** Critic -> revise loop. */
    private Mono<String> reflect(String userRequest, String draft, int iteration) {
        if (iteration >= MAX_REVISIONS) {
            log.info("[CodeGen] reached max revisions ({}), returning draft", MAX_REVISIONS);
            tracing.tag("code_gen.iterations", iteration);
            return Mono.just(draft);
        }
        return critic.review(userRequest, draft)
                .flatMap(critique -> {
                    if (critique.approved()) {
                        log.info("[CodeGen] critic approved at iteration {}", iteration);
                        tracing.tag("code_gen.iterations", iteration);
                        tracing.tag("code_gen.approved", "true");
                        return Mono.just(draft);
                    }
                    log.info("[CodeGen] critic rejected at iteration {} ({} issues) - revising",
                            iteration, critique.issues() == null ? 0 : critique.issues().size());
                    return revise(userRequest, draft, critique, iteration + 1)
                            .flatMap(revised -> reflect(userRequest, revised, iteration + 1));
                });
    }

    private static String bullets(List<String> items) {
        if (items == null || items.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (String s : items) sb.append("- ").append(s).append('\n');
        return sb.toString();
    }
}
