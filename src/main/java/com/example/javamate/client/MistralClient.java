package com.example.javamate.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class MistralClient {

    private final ChatClient chatClient;

    public MistralClient(MistralAiChatModel mistralAiChatModel) {

        ChatOptions options = ChatOptions.builder()
                .temperature(0.2)
                .build();

        this.chatClient = ChatClient.builder(mistralAiChatModel)
                .defaultOptions(options)
                .build();
    }

    // Blocking call - returns complete response
    public String ask(Prompt prompt) {
        return chatClient.prompt(prompt).call().content();
    }


     // Streaming call - returns Flux of response chunks for real-time streaming
    public Flux<String> askStream(Prompt prompt) {
        return chatClient.prompt(prompt).stream().content();
    }
}

