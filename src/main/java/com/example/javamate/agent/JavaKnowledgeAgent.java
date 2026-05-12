package com.example.javamate.agent;

import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tracing.AgentTracing;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;


@Component
public class JavaKnowledgeAgent implements Agent {

    private final ChatClient chat;
    private final AgentTracing tracing;

    public JavaKnowledgeAgent(MistralAiChatModel model, AgentTracing tracing) {
        this.tracing = tracing;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.JAVA_KNOWLEDGE)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    @Override
    public AgentName name() {
        return AgentName.JAVA_KNOWLEDGE;
    }

    @Override
    public Mono<AgentResult> run(AgentContext ctx) {
        Map<String, String> tags = Map.of(
                "agent.name", "JAVA_KNOWLEDGE",
                "agent.input.chars", String.valueOf(ctx.query().length()));
        return tracing.wrap("agent.java_knowledge", tags,
                Mono.fromCallable(() -> chat.prompt().user(ctx.query()).call().content())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(answer -> {
                            tracing.tag("agent.output.chars", answer == null ? 0 : answer.length());
                            return new AgentResult(name(), answer);
                        }));
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        Map<String, String> tags = Map.of(
                "agent.name", "JAVA_KNOWLEDGE",
                "agent.input.chars", String.valueOf(ctx.query().length()));
        return tracing.wrap("agent.java_knowledge.stream", tags,
                chat.prompt().user(ctx.query()).stream().content());
    }
}
