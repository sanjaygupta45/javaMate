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
public class InterviewPrepAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(InterviewPrepAgent.class);
    private static final int FALLBACK_MAX_RESULTS = 5;

    private final ChatClient chat;
    private final ChatClient fallbackChat;
    private final WebSearchTools tools;
    private final WebSearchClient searchClient;
    private final AgentTracing tracing;

    public InterviewPrepAgent(MistralAiChatModel model,
                              WebSearchTools tools,
                              WebSearchClient searchClient,
                              AgentTracing tracing) {
        this.tracing = tracing;
        this.tools = tools;
        this.searchClient = searchClient;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.INTERVIEW_PREP)
                .defaultOptions(ChatOptions.builder().temperature(0.3).build())
                .defaultTools(tools)
                .build();
        this.fallbackChat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.INTERVIEW_PREP)
                .defaultOptions(ChatOptions.builder().temperature(0.3).build())
                .build();
    }

    @Override
    public AgentName name() {
        return AgentName.INTERVIEW_PREP;
    }

    @Override
    public Mono<AgentResult> run(AgentContext ctx) {
        Map<String, String> tags = Map.of(
                "agent.name", "INTERVIEW_PREP",
                "agent.input.chars", String.valueOf(ctx.query().length()));
        return tracing.wrap("agent.interview_prep", tags,
                Mono.fromCallable(() -> {
                            AgentCallContext.set(ctx.conversationId());
                            try {
                                try {
                                    return chat.prompt().user(ctx.query()).call().content();
                                } catch (IllegalStateException ise) {
                                    if (isContentNotAStringError(ise)) {
                                        log.warn("[InterviewPrepAgent] Mistral 'content is not a string' "
                                                + "bug hit; falling back. msg={}", ise.getMessage());
                                        tracing.tag("agent.fallback", "content_not_a_string");
                                        return fallbackAnswer(ctx);
                                    }
                                    throw ise;
                                } catch (RuntimeException re) {
                                    Throwable cause = re.getCause();
                                    if (cause instanceof IllegalStateException ise2
                                            && isContentNotAStringError(ise2)) {
                                        log.warn("[InterviewPrepAgent] Mistral 'content is not a string' "
                                                + "bug (wrapped); falling back.");
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
        // Tool-calling + streaming is fragile; emit the complete answer as one chunk.
        return run(ctx).flatMapMany(r -> Flux.just(r.content()));
    }

    // ---------------- fallback path ----------------

    private static boolean isContentNotAStringError(Throwable t) {
        String msg = t == null ? null : t.getMessage();
        return msg != null && msg.toLowerCase().contains("content is not a string");
    }

    private String fallbackAnswer(AgentContext ctx) {
        String resultsBlock;
        String searchQuery = "top 10 most asked Java interview questions for freshers "
                + "(scenario based, spring boot) - " + ctx.query();
        try {
            resultsBlock = tools.webSearch(searchQuery);
        } catch (Exception searchErr) {
            log.warn("[InterviewPrepAgent] fallback search failed: {}", searchErr.getMessage());
            List<WebSearchClient.SearchResult> raw =
                    searchClient.search(searchQuery, FALLBACK_MAX_RESULTS);
            if (raw == null || raw.isEmpty()) {
                return "I couldn't fetch fresh interview-prep data right now. "
                        + "Please try again in a moment.";
            }
            StringBuilder sb = new StringBuilder();
            for (WebSearchClient.SearchResult r : raw) {
                sb.append("- ").append(r.title()).append(" (").append(r.url()).append(")\n  ")
                        .append(r.snippet()).append('\n');
            }
            resultsBlock = sb.toString();
        }

        String userPrompt = """
                User question:
                %s

                Reference web search results (current industry interview pattern):
                %s

                Following your system instructions, produce a beginner-friendly interview-prep
                answer for a fresher / 0-2 yrs Java backend developer. If the user asked for
                top / common questions, give ~10 with short crisp answers and tiny code where
                useful. For one-off questions, follow the direct-answer / why-asked / example /
                follow-ups / red-flags structure. Cite the most useful URLs inline.
                """.formatted(ctx.query(), resultsBlock);

        try {
            String answer = fallbackChat.prompt().user(userPrompt).call().content();
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
        } catch (Exception e) {
            log.warn("[InterviewPrepAgent] fallback synthesis failed: {}", e.getMessage());
        }
        return "Here is what I found about current Java interview patterns:\n\n" + resultsBlock;
    }
}

