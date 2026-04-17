package com.example.javamate.client;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class MistralClient {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;


    public MistralClient(MistralAiChatModel mistralAiChatModel, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;

        ChatOptions options = ChatOptions.builder()
                .temperature(0.2)
                .build();

        // Basic client - memory advisor is added per-request with specific conversationId
        this.chatClient = ChatClient.builder(mistralAiChatModel)
                .defaultOptions(options)
                .build();
    }


    public String askWithMemorySkipUserSave(Prompt prompt, String conversationId) {
        // Create a custom chat memory that skips user message saving
        ChatMemory skipUserSaveMemory = new SkipUserSaveChatMemory(chatMemory);
        
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(skipUserSaveMemory)
                .conversationId(conversationId)
                .build();

        return chatClient.prompt(prompt)
                .advisors(memoryAdvisor)
                .call()
                .content();
    }

    public Flux<String> askStreamWithMemorySkipUserSave(Prompt prompt, String conversationId) {
        ChatMemory skipUserSaveMemory = new SkipUserSaveChatMemory(chatMemory);
        
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(skipUserSaveMemory)
                .conversationId(conversationId)
                .build();

        return chatClient.prompt(prompt)
                .advisors(memoryAdvisor)
                .stream()
                .content();
    }


    /**
     * Custom ChatMemory wrapper that skips saving UserMessages.
     * 
     * This is used because:
     * 1. Raw user queries are saved manually BEFORE LLM call (in AIServiceImpl)
     * 2. MessageChatMemoryAdvisor would otherwise save the formatted prompt (with RAG context)
     * 3. We only want the advisor to save AssistantMessages after LLM response
     * 
     * Flow:
     * - User sends: "how does garbage collection work"
     * - AIServiceImpl saves raw query to DB
     * - buildPrompt() creates: "Question: how does garbage collection work\n\nRelevant Context: ..."
     * - This wrapper intercepts add() calls and filters out UserMessages
     * - Only AssistantMessage (LLM response) gets saved via delegate
     */
    private record SkipUserSaveChatMemory(ChatMemory delegate) implements ChatMemory {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkipUserSaveChatMemory.class);

        @Override
        public void add(@NonNull String conversationId, java.util.List<org.springframework.ai.chat.messages.Message> messages) {
            // Filter out UserMessages - only save AssistantMessages
            // UserMessages are already saved with raw query content in AIServiceImpl
            java.util.List<org.springframework.ai.chat.messages.Message> assistantMessages = messages.stream()
                    .filter(m -> m instanceof org.springframework.ai.chat.messages.AssistantMessage)
                    .toList();
            
            int skippedCount = messages.size() - assistantMessages.size();
            if (skippedCount > 0) {
                log.debug("Skipped {} UserMessage(s) from being saved (raw query already saved)", skippedCount);
            }
            
            if (!assistantMessages.isEmpty()) {
                log.debug("Saving {} AssistantMessage(s) to memory for conversation {}", 
                        assistantMessages.size(), conversationId);
                delegate.add(conversationId, assistantMessages);
            }
        }

        @Override
        public java.util.List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
            return delegate.get(conversationId);
        }

        @Override
        public void clear(@NonNull String conversationId) {
            delegate.clear(conversationId);
        }
    }
}
