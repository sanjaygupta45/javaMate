package com.example.javamate.agent;

import com.example.javamate.agent.events.AgentEventBus;
import com.example.javamate.agent.prompts.AgentPrompts;
import com.example.javamate.agent.tracing.AgentTracing;
import com.example.javamate.agent.events.AgentStreamEvent;
import com.example.javamate.service.VectorDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class PersonalRagAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PersonalRagAgent.class);
    private static final int SIMILARITY_LIMIT = 30;
    private static final String CONTEXT_SEPARATOR = "\n\n---\n\n";

    private final ChatClient chat;
    private final VectorDatabaseService vectorDatabaseService;
    private final AgentTracing tracing;
    private final AgentEventBus eventBus;

    public PersonalRagAgent(MistralAiChatModel model,
                            VectorDatabaseService vectorDatabaseService,
                            AgentTracing tracing,
                            AgentEventBus eventBus) {
        this.vectorDatabaseService = vectorDatabaseService;
        this.tracing = tracing;
        this.eventBus = eventBus;
        this.chat = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.PERSONAL_RAG)
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    @Override
    public AgentName name() {
        return AgentName.PERSONAL_RAG;
    }

    @Override
    public Mono<AgentResult> run(AgentContext ctx) {
        return tracing.wrap(
                "agent.personal_rag",
                Map.of("agent.name", "PERSONAL_RAG", "user.id", String.valueOf(ctx.userId())),
                buildUserPrompt(ctx)
                        .flatMap(p -> Mono.fromCallable(() -> chat.prompt().user(p).call().content())
                                .subscribeOn(Schedulers.boundedElastic()))
                        .map(answer -> {
                            tracing.tag("agent.output.chars", answer == null ? 0 : answer.length());
                            return new AgentResult(name(), answer);
                        })
        );
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        return tracing.wrap(
                "agent.personal_rag.stream",
                Map.of("agent.name", "PERSONAL_RAG", "user.id", String.valueOf(ctx.userId())),
                buildUserPrompt(ctx)
                        .flatMapMany(p -> chat.prompt().user(p).stream().content())
        );
    }

    private Mono<String> buildUserPrompt(AgentContext ctx) {
        return Mono.fromCallable(() -> tracing.span(
                "agent.personal_rag.retrieve",
                Map.of("rag.top_k", String.valueOf(SIMILARITY_LIMIT)),
                () -> {
                    List<Document> docs = vectorDatabaseService.similaritySearchByUserId(
                            ctx.query(), SIMILARITY_LIMIT, ctx.userId());
                    tracing.tag("rag.docs.found", docs.size());
                    log.debug("[PersonalRagAgent] retrieved {} chunks for userId={}", docs.size(), ctx.userId());

                    // Emit a tool event so the UI can show citations / "looking in your notes".
                    List<Map<String, String>> citations = docs.stream()
                            .map(d -> {
                                Map<String, String> m = new LinkedHashMap<>();
                                m.put("documentId", d.getId() == null ? "" : d.getId());
                                m.put("snippet", snippet(d.getText()));
                                return m;
                            })
                            .toList();
                    Map<String, Object> args = new LinkedHashMap<>();
                    args.put("query", ctx.query());
                    args.put("topK", SIMILARITY_LIMIT);
                    eventBus.emit(ctx.conversationId(),
                            new AgentStreamEvent.ToolEvent("personal_rag.retrieve", args, citations));

                    String context = docs.isEmpty()
                            ? "(no personal documents matched)"
                            : docs.stream().map(Document::getText).collect(Collectors.joining(CONTEXT_SEPARATOR));
                    return "Question:\n" + ctx.query() + "\n\nContext from the user's personal knowledge base:\n" + context;
                })).subscribeOn(Schedulers.boundedElastic());
    }

    private static String snippet(String text) {
        if (text == null) return "";
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= 200 ? t : t.substring(0, 200) + "...";
    }
}
