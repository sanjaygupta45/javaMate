package com.example.javamate.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Component;



@Component
public class VertexClient {

    private final ChatClient chatClient;

    public VertexClient(ChatClient.Builder builder) {

        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .temperature(0.2)
                .build();

        this.chatClient = builder
                .defaultOptions(options)
                .build();
    }

    public String ask(Prompt prompt) {
       return  chatClient.prompt(prompt).call().content();
    }

}