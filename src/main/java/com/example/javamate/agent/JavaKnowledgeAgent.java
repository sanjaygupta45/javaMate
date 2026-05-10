package com.example.javamate.agent;

import com.example.javamate.agent.prompts.AgentPrompts;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Pure-LLM Java/Spring/JVM expert. No external data, no tools.
 */
@Component
public class JavaKnowledgeAgent implements Agent {

    private final ChatClient chat;

    public JavaKnowledgeAgent(MistralAiChatModel model) {
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
        return Mono.fromCallable(() -> chat.prompt().user(ctx.query()).call().content())
                .subscribeOn(Schedulers.boundedElastic())
                .map(answer -> new AgentResult(name(), answer));
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        return chat.prompt().user(ctx.query()).stream().content();
    }
}
