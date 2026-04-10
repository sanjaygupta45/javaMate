package com.example.javamate.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;


@Component
public class DeepSeekClient {

    private final ChatClient chatClient;

    public DeepSeekClient(ChatClient.Builder builder) {

        ChatOptions options = ChatOptions.builder()
                .temperature(0.2)
                .build();

        this.chatClient = builder
                .defaultOptions(options)
                .build();
    }

    public String ask(Prompt prompt) {
        return chatClient.prompt(prompt).call().content();
    }

}

