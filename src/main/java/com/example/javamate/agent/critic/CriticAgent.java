package com.example.javamate.agent.critic;

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
 * Reviews a code draft and produces a {@link CritiqueResult}.
 *
 * <p>NOT an {@link com.example.javamate.agent.Agent} - it is a private collaborator of
 * {@link com.example.javamate.agent.CodeGenAgent} (reflection pattern). The Supervisor
 * never routes to the Critic directly.
 */
@Component
public class CriticAgent {

    private static final Logger log = LoggerFactory.getLogger(CriticAgent.class);

    private final ChatClient chat;
    private final AgentTracing tracing;

    public CriticAgent(MistralAiChatModel model, AgentTracing tracing) {
        this.tracing = tracing;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.CRITIC)
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .build();
    }

    public Mono<CritiqueResult> review(String userRequest, String draft) {
        return Mono.fromCallable(() -> tracing.span(
                "agent.critic",
                Map.of("agent.name", "CRITIC", "agent.input.chars", String.valueOf(draft.length())),
                () -> {
                    String userPrompt = "Original user request:\n" + userRequest
                            + "\n\nDraft answer to review:\n" + draft;
                    CritiqueResult result;
                    try {
                        result = chat.prompt().user(userPrompt).call().entity(CritiqueResult.class);
                    } catch (Exception e) {
                        log.warn("[Critic] structured parse failed - approving draft. cause={}", e.getMessage());
                        return new CritiqueResult(true, List.of(), "");
                    }
                    if (result == null) {
                        return new CritiqueResult(true, List.of(), "");
                    }
                    tracing.tag("critic.approved", String.valueOf(result.approved()));
                    tracing.tag("critic.issues.count",
                            String.valueOf(result.issues() == null ? 0 : result.issues().size()));
                    log.info("[Critic] approved={} issues={}",
                            result.approved(),
                            result.issues() == null ? 0 : result.issues().size());
                    return result;
                })).subscribeOn(Schedulers.boundedElastic());
    }
}
