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
                You are JavaMate - an AI-powered Java development assistant.
                
                Your goal is to help developers with clear, correct, and practical answers.
                
                Rules:
                1. If relevant context is provided, use it as the primary source of truth.
                2. If the context is incomplete or missing, use your own knowledge to answer.
                3. Always prefer accurate and practical solutions (code examples if helpful).
                4. Do NOT say "I couldn't find that information" unless the question is truly unanswerable.
                5. If using your own knowledge instead of context, ensure the answer is generally accepted best practice.
                6. Keep answers concise but useful for a Java developer.
                7. When appropriate, explain reasoning briefly.
                
                You are allowed to combine context + your own knowledge when needed.
                """;

        String userPrompt = """
                Context:
                %s
                
                Question:
                %s
                
                Instructions:
                - If context is relevant, use it.
                - If not, answer using your own knowledge.
                """.formatted(context, userQuery);

        return new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                )
        );
    }

}
