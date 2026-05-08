package com.example.javamate.config;

import com.example.javamate.entity.ChatMessage;
import com.example.javamate.service.ChatMemoryService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ChatMemoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryConfig.class);

    @Value("${javamate.chat.memory.window-size:20}")
    private int memoryWindowSize;

    // Threshold: after this many messages, we trigger summarization
    @Value("${javamate.chat.memory.compact-threshold:5}")
    private int compactThreshold;


    @Bean
    public ChatMemory chatMemory(ChatMemoryService chatMemoryService, MistralAiChatModel mistralAiChatModel) {
        return new DatabaseBackedChatMemory(chatMemoryService, memoryWindowSize, compactThreshold, mistralAiChatModel);
    }


    private record DatabaseBackedChatMemory(
            ChatMemoryService chatMemoryService,
            int windowSize,
            int compactThreshold,
            MistralAiChatModel chatModel  // Reuse existing model for summarization
    ) implements ChatMemory {

        private static final Logger log = LoggerFactory.getLogger(DatabaseBackedChatMemory.class);

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

            // Check if we need to compact memory after adding messages
            compactMemoryIfNeeded(userId, sessionId);
        }

        /**
         * Compact memory by summarizing older messages when threshold is exceeded.
         * This keeps recent context while preserving important information from older messages.
         */
        private void compactMemoryIfNeeded(Long userId, String sessionId) {
            try {
                Long messageCount = chatMemoryService.getNonSummaryMessageCount(userId, sessionId).block();

                // Only compact when we have more than threshold messages
                if (messageCount == null || messageCount <= compactThreshold) {
                    return;
                }

                log.info("Compacting memory for session {} - {} messages exceed threshold of {}",
                        sessionId, messageCount, compactThreshold);

                // Get oldest messages to summarize (keep recent ones intact)
                int messagesToSummarize = Math.toIntExact(messageCount - compactThreshold);
                List<ChatMessage> oldMessages = chatMemoryService
                        .getOldestMessagesForCompaction(userId, sessionId, messagesToSummarize)
                        .collectList()
                        .block();

                if (oldMessages == null || oldMessages.isEmpty()) {
                    return;
                }

                // Generate summary using existing model
                String summary = generateSummary(oldMessages);

                // Save summary as a special SUMMARY message
                chatMemoryService.saveSummaryMessage(userId, sessionId, summary).block();

                // Delete the old messages that were summarized
                List<Long> messageIds = oldMessages.stream()
                        .map(ChatMessage::getMessageId)
                        .collect(Collectors.toList());
                chatMemoryService.deleteMessagesByIds(messageIds).block();

                log.info("Compacted {} messages into summary for session {}", oldMessages.size(), sessionId);

            } catch (Exception e) {
                // Log error but don't fail the main operation
                log.error("Failed to compact memory for session: {}", e.getMessage());
            }
        }

        /**
         * Generate a summary of older messages using the existing LLM.
         */
        private String generateSummary(List<ChatMessage> messages) {
            StringBuilder conversationText = new StringBuilder();
            for (ChatMessage msg : messages) {
                conversationText.append(msg.getRole())
                        .append(": ")
                        .append(msg.getContent())
                        .append("\n");
            }

            // Prompt for summarization - asking the model to preserve key context
            String summaryPrompt = """
                    Summarize the following conversation concisely, preserving key topics, 
                    questions asked, and important context that would be useful for continuing 
                    the conversation. Keep it brief but informative.
                    
                    Conversation:
                    %s
                    
                    Summary:""".formatted(conversationText.toString());

            Prompt prompt = new Prompt(summaryPrompt);
            String summary = chatModel.call(prompt).getResult().getOutput().getText();

            log.debug("Generated summary: {}", summary);
            return summary;
        }

        @Override
        public @Nullable List<Message> get(@NonNull String conversationId) {
            String[] parts = parseConversationId(conversationId);
            Long userId = Long.parseLong(parts[0]);
            String sessionId = parts[1];

            // Get all messages including SUMMARY messages
            List<Message> messages = chatMemoryService.getRecentMessages(userId, sessionId, windowSize)
                    .map(this::toSpringAiMessage)
                    .collect(Collectors.toList())
                    .block();

            return messages;
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
                // SUMMARY messages are returned as SystemMessage to provide context
                case "SUMMARY" -> new SystemMessage("Previous conversation summary: " + chatMessage.getContent());
                default -> throw new IllegalArgumentException("Unknown role: " + chatMessage.getRole());
            };
        }
    }
}

