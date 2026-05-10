package com.example.javamate.agent;

import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tools.WebSearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Answers questions that need fresh / live information. Owns the web-search tool.
 * Spring AI auto-loops between the LLM and the @Tool method until the LLM emits a final answer.
 */
@Component
public class WebSearchAgent implements Agent {

    private final ChatClient chat;

    public WebSearchAgent(MistralAiChatModel model, WebSearchTools tools) {
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
        return Mono.fromCallable(() -> chat.prompt().user(ctx.query()).call().content())
                .subscribeOn(Schedulers.boundedElastic())
                .map(answer -> new AgentResult(name(), answer));
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        // Streaming + tool-calling can be tricky; for safety we use blocking call here
        // and emit the full answer as a single chunk. Multi-agent path goes through
        // the synthesizer anyway, which streams cleanly.
        return run(ctx).flatMapMany(r -> Flux.just(r.content()));
    }
}
