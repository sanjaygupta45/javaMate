package com.example.javamate.agent;

import com.example.javamate.agent.events.AgentCallContext;
import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tools.WebSearchClient;
import com.example.javamate.agent.tools.WebSearchTools;
import com.example.javamate.agent.tracing.AgentTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;


@Component
public class WebSearchAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(WebSearchAgent.class);
    private static final int FALLBACK_MAX_RESULTS = 5;

    private final ChatClient chat;
    /**
     * No-tools ChatClient used as a fallback path when the tool-calling loop
     * trips the Mistral "content is not a string" bug (Spring AI 2.0.0-M2:
     * MistralAiApi.ChatCompletionMessage.content() throws IllegalStateException
     * when Mistral returns content as a list of content parts instead of a plain
     * string after several tool-call rounds).
     */
    private final ChatClient fallbackChat;
    private final WebSearchTools tools;
    private final WebSearchClient searchClient;
    private final AgentTracing tracing;

    public WebSearchAgent(MistralAiChatModel model,
                          WebSearchTools tools,
                          WebSearchClient searchClient,
                          AgentTracing tracing) {
        this.tracing = tracing;
        this.tools = tools;
        this.searchClient = searchClient;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.WEB_SEARCH)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .defaultTools(tools)
                .build();
        this.fallbackChat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.WEB_SEARCH)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
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
                                try {
                                    return chat.prompt().user(ctx.query()).call().content();
                                } catch (IllegalStateException ise) {
                                    // Spring AI Mistral M2 bug: assistant message content
                                    // sometimes comes back as a list of content parts after
                                    // multiple tool-call rounds, which the SDK rejects.
                                    // Fall back to a direct search + summarise path.
                                    if (isContentNotAStringError(ise)) {
                                        log.warn("[WebSearchAgent] Mistral 'content is not a string' bug hit; "
                                                + "falling back to direct search synthesis. msg={}",
                                                ise.getMessage());
                                        tracing.tag("agent.fallback", "content_not_a_string");
                                        return fallbackAnswer(ctx);
                                    }
                                    throw ise;
                                } catch (RuntimeException re) {
                                    // Sometimes the SDK wraps the IllegalStateException.
                                    Throwable cause = re.getCause();
                                    if (cause instanceof IllegalStateException ise2
                                            && isContentNotAStringError(ise2)) {
                                        log.warn("[WebSearchAgent] Mistral 'content is not a string' bug "
                                                + "(wrapped); falling back to direct search synthesis.");
                                        tracing.tag("agent.fallback", "content_not_a_string");
                                        return fallbackAnswer(ctx);
                                    }
                                    throw re;
                                }
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

    // -----------------------------------------------------------------
    // Fallback path: bypass the tool-calling loop entirely.
    // -----------------------------------------------------------------

    private static boolean isContentNotAStringError(Throwable t) {
        String msg = t == null ? null : t.getMessage();
        return msg != null && msg.toLowerCase().contains("content is not a string");
    }

    private String fallbackAnswer(AgentContext ctx) {
        // 1) Run a single search directly. Calling the @Tool method publicly is
        //    fine - it also emits the ToolEvent so the UI still sees the sources.
        String resultsBlock;
        try {
            resultsBlock = tools.webSearch(ctx.query());
        } catch (Exception searchErr) {
            log.warn("[WebSearchAgent] fallback search failed: {}", searchErr.getMessage());
            // Last resort: direct client call, no event emission.
            List<WebSearchClient.SearchResult> raw =
                    searchClient.search(ctx.query(), FALLBACK_MAX_RESULTS);
            if (raw == null || raw.isEmpty()) {
                return "I tried to search the web but couldn't retrieve fresh results just now. "
                        + "Please try again in a moment.";
            }
            StringBuilder sb = new StringBuilder();
            for (WebSearchClient.SearchResult r : raw) {
                sb.append("- ").append(r.title()).append(" (").append(r.url()).append(")\n  ")
                        .append(r.snippet()).append('\n');
            }
            resultsBlock = sb.toString();
        }

        // 2) Ask the model to synthesise an answer from the provided results,
        //    with NO tools attached so we never re-enter the broken code path.
        String userPrompt = """
                Question:
                %s

                Web search results:
                %s

                Write a concise, accurate answer to the question grounded in these results.
                Cite the most relevant URLs inline as markdown links. If the results don't
                actually answer the question, say so honestly instead of guessing.
                """.formatted(ctx.query(), resultsBlock);

        try {
            String answer = fallbackChat.prompt().user(userPrompt).call().content();
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
        } catch (Exception e) {
            log.warn("[WebSearchAgent] fallback synthesis failed: {}", e.getMessage());
        }

        // 3) If the LLM is also unhappy, return the raw results so the user gets *something*.
        return "Here is what I found on the web:\n\n" + resultsBlock;
    }
}
