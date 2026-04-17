package com.example.javamate.config;

import com.example.javamate.entity.ChatMessage;
import com.example.javamate.service.ChatMemoryService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ChatMemoryConfig {

    @Value("${javamate.chat.memory.window-size:20}")
    private int memoryWindowSize;


    @Bean
    public ChatMemory chatMemory(ChatMemoryService chatMemoryService) {
        return new DatabaseBackedChatMemory(chatMemoryService, memoryWindowSize);
    }


    private record DatabaseBackedChatMemory(ChatMemoryService chatMemoryService, int windowSize) implements ChatMemory {

        @Override
            public void add(@NonNull String conversationId, List<Message> messages) {

                String[] parts = parseConversationId(conversationId);
                Long userId = Long.parseLong(parts[0]);
                String sessionId = parts[1];

                for (Message message : messages) {
                    if (message instanceof UserMessage) {
                        chatMemoryService.saveUserMessage(userId, sessionId, message.getText())
                                .block(); // Blocking for ChatMemory interface compatibility
                    } else if (message instanceof AssistantMessage) {
                        chatMemoryService.saveAssistantMessage(userId, sessionId, message.getText())
                                .block();
                    }
                }
            }

            @Override
            public @Nullable List<Message> get(@NonNull String conversationId) {
                String[] parts = parseConversationId(conversationId);
                Long userId = Long.parseLong(parts[0]);
                String sessionId = parts[1];

                return chatMemoryService.getRecentMessages(userId, sessionId, windowSize)
                        .map(this::toSpringAiMessage)
                        .collect(Collectors.toList())
                        .block(); // Blocking for ChatMemory interface compatibility
            }

            @Override
            public void clear(@NonNull String conversationId) {
                String[] parts = parseConversationId(conversationId);
                Long userId = Long.parseLong(parts[0]);
                String sessionId = parts[1];

                chatMemoryService.clearSession(userId, sessionId).block();
            }

            private String[] parseConversationId(String conversationId) {
                String[] parts = conversationId.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                            "Invalid conversationId format. Expected 'userId:sessionId', got: " + conversationId);
                }
                return parts;
            }

            private Message toSpringAiMessage(ChatMessage chatMessage) {
                return switch (chatMessage.getRole()) {
                    case "USER" -> new UserMessage(chatMessage.getContent());
                    case "ASSISTANT" -> new AssistantMessage(chatMessage.getContent());
                    default -> throw new IllegalArgumentException("Unknown role: " + chatMessage.getRole());
                };
            }
        }
}

