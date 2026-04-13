package com.example.javamate.service.impl;

import com.example.javamate.client.MistralClient;
import com.example.javamate.service.AIService;
import com.example.javamate.service.VectorDatabaseService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@AllArgsConstructor
public class AIServiceImpl implements AIService {

    private final MistralClient mistralClient;
    private final VectorDatabaseService vectorDatabaseService;

    @Override
    public Mono<String> generateResponse(String userQuery) {
        return Mono.fromCallable(() -> {
            List<Document> docs = vectorDatabaseService.similaritySearch(userQuery, 5);
            Prompt prompt = buildPrompt(userQuery, docs);
            return mistralClient.ask(prompt);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> generateStreamingResponse(String userQuery) {
        return Mono.fromCallable(() -> {
            List<Document> docs = vectorDatabaseService.similaritySearch(userQuery, 5);
            return buildPrompt(userQuery, docs);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(mistralClient::askStream);
    }

    private Prompt buildPrompt(String userQuery, List<Document> documents) {

        String context = documents.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        String systemPrompt = """
                You are JavaMate - an AI-powered Java development co-learner and assistant.

                Use ONLY the provided context to answer the user question.

                If the answer cannot be found in the context,
                reply with: "I couldn't find that information in the knowledge base."

                Be concise, accurate, and helpful for Java developers.
                """;

        String userPrompt = """
                Context:
                %s

                Question:
                %s
                """.formatted(context, userQuery);

        return new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                )
        );
    }
}