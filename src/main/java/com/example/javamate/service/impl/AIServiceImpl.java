package com.example.javamate.service.impl;

import com.example.javamate.client.VertexClient;
import com.example.javamate.service.AIService;
import com.example.javamate.service.VectorDatabaseService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AIServiceImpl implements AIService {

    private final VertexClient vertexClient;
    private final VectorDatabaseService vectorDatabaseService;

    @Override
    public String generateResponse(String userQuery) {

        List<Document> docs = vectorDatabaseService.similaritySearch(userQuery, 5);

        Prompt prompt = buildPrompt(userQuery, docs);

        return vertexClient.ask(prompt);
    }

    private Prompt buildPrompt(String userQuery, List<Document> documents) {

        String context = documents.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        String systemPrompt = """
                You are an AI travel assistant for flight bookings.

                Use ONLY the provided context to answer the user question.

                If the answer cannot be found in the context,
                reply with: "I couldn't find that information in the knowledge base."

                Be concise and accurate.
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