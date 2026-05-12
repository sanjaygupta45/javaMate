package com.example.javamate.agent;

import com.example.javamate.agent.events.AgentCallContext;
import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tools.WebSearchTools;
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
public class WebSearchAgent implements Agent {

    private final ChatClient chat;
    private final AgentTracing tracing;

    public WebSearchAgent(MistralAiChatModel model, WebSearchTools tools, AgentTracing tracing) {
        this.tracing = tracing;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.WEB_SEARCH)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .defaultTools(tools)
                .build();
    }

    @Override
    public AgentName name() {
        return AgentName.WEB_SEARCH;
    }

    @Override
    public Mono<AgentResult> run(AgentContext ctx) {
        Map<String, String> tags = Map.of(
                "agent.name", "WEB_SEARCH",
                "agent.input.chars", String.valueOf(ctx.query().length()));
        return tracing.wrap("agent.web_search", tags,
                Mono.fromCallable(() -> {
                            AgentCallContext.set(ctx.conversationId());
                            try {
                                return chat.prompt().user(ctx.query()).call().content();
                            } finally {
                                AgentCallContext.clear();
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(answer -> {
                            tracing.tag("agent.output.chars", answer == null ? 0 : answer.length());
                            return new AgentResult(name(), answer);
                        }));
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        // Streaming + tool-calling can be tricky; safer to do blocking call and emit one chunk.
        return run(ctx).flatMapMany(r -> Flux.just(r.content()));
    }
}
