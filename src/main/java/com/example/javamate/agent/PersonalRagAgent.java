package com.example.javamate.agent;

import com.example.javamate.agent.prompts.AgentPrompts;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Answers using the user's personal uploaded documents (RAG over Qdrant).
 */
@Component
public class PersonalRagAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PersonalRagAgent.class);
    private static final int SIMILARITY_LIMIT = 30;
    private static final String CONTEXT_SEPARATOR = "\n\n---\n\n";

    private final ChatClient chat;
    private final VectorDatabaseService vectorDatabaseService;

    public PersonalRagAgent(MistralAiChatModel model, VectorDatabaseService vectorDatabaseService) {
        this.vectorDatabaseService = vectorDatabaseService;
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
        return buildUserPrompt(ctx)
                .flatMap(p -> Mono.fromCallable(() -> chat.prompt().user(p).call().content())
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(answer -> new AgentResult(name(), answer));
    }

    @Override
    public Flux<String> stream(AgentContext ctx) {
        return buildUserPrompt(ctx)
                .flatMapMany(p -> chat.prompt().user(p).stream().content());
    }

    private Mono<String> buildUserPrompt(AgentContext ctx) {
        return Mono.fromCallable(() -> {
            List<Document> docs = vectorDatabaseService.similaritySearchByUserId(
                    ctx.query(), SIMILARITY_LIMIT, ctx.userId());
            log.debug("[PersonalRagAgent] retrieved {} chunks for userId={}", docs.size(), ctx.userId());
            String context = docs.isEmpty()
                    ? "(no personal documents matched)"
                    : docs.stream().map(Document::getText).collect(Collectors.joining(CONTEXT_SEPARATOR));
            return "Question:\n" + ctx.query() + "\n\nContext from the user's personal knowledge base:\n" + context;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
