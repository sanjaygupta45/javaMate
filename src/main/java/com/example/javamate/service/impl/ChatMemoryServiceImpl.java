package com.example.javamate.service.impl;

import com.example.javamate.dto.ChatSessionDTO;
import com.example.javamate.entity.ChatMessage;
import com.example.javamate.repository.ChatMessageRepository;
import com.example.javamate.service.ChatMemoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ChatMemoryServiceImpl implements ChatMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryServiceImpl.class);

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private final ChatMessageRepository chatMessageRepository;

    @Value("${javamate.chat.memory.default-window-size:20}")
    private int defaultWindowSize;

    @Value("${javamate.chat.memory.max-messages-per-session:20}")
    private int maxMessagesPerSession;

    @Override
    public Mono<ChatMessage> saveUserMessage(Long userId, String sessionId, String content) {
        // Session limit is checked at the service layer (JavaMateServiceImpl)
        // This method is called by Spring AI's MessageChatMemoryAdvisor
        return saveMessage(userId, sessionId, ROLE_USER, content);
    }

    @Override
    public Mono<ChatMessage> saveAssistantMessage(Long userId, String sessionId, String content) {
        // Called by Spring AI's MessageChatMemoryAdvisor after response generation
        return saveMessage(userId, sessionId, ROLE_ASSISTANT, content);
    }

    private Mono<ChatMessage> saveMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage message = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(message)
                .doOnSuccess(saved -> logger.debug("Saved {} message for session {}", role, sessionId))
                .doOnError(error -> logger.error("Failed to save {} message for session {}: {}",
                        role, sessionId, error.getMessage()));
    }

    @Override
    public Flux<ChatMessage> getRecentMessages(Long userId, String sessionId, int limit) {
        int effectiveLimit = limit > 0 ? limit : defaultWindowSize;
        return chatMessageRepository.findRecentMessages(userId, sessionId, effectiveLimit)
                .doOnSubscribe(sub -> logger.debug("Fetching last {} messages for session {}", effectiveLimit, sessionId));
    }

    @Override
    public Flux<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        return chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
    }

    @Override
    public Mono<Void> clearSession(Long userId, String sessionId) {
        return chatMessageRepository.deleteByUserIdAndSessionId(userId, sessionId)
                .doOnSuccess(v -> logger.info("Cleared chat history for session {}", sessionId))
                .doOnError(error -> logger.error("Failed to clear session {}: {}", sessionId, error.getMessage()));
    }

    @Override
    public Mono<Void> clearAllUserSessions(Long userId) {
        return chatMessageRepository.deleteByUserId(userId)
                .doOnSuccess(v -> logger.info("Cleared all chat history for user {}", userId));
    }

    @Override
    public Flux<ChatSessionDTO> listUserSessions(Long userId) {
        return chatMessageRepository.findDistinctSessionIdsByUserId(userId)
                .flatMap(sessionId -> 
                    Mono.zip(
                        chatMessageRepository.countByUserIdAndSessionId(userId, sessionId),
                        chatMessageRepository.findLatestMessageInSession(userId, sessionId)
                            .map(ChatMessage::getCreatedAt)
                            .defaultIfEmpty(LocalDateTime.now())
                    ).map(tuple -> ChatSessionDTO.builder()
                            .sessionId(sessionId)
                            .messageCount(tuple.getT1())
                            .lastMessageAt(tuple.getT2())
                            .build())
                );
    }

    @Override
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    

    @Override
    public Mono<Long> getUserMessageCount(Long userId, String sessionId) {
        // Returns only USER message count for limit checking
        return chatMessageRepository.countUserMessagesByUserIdAndSessionId(userId, sessionId);
    }

    @Override
    public Mono<Boolean> isSessionLimitReached(Long userId, String sessionId) {
        // Limit is based on USER messages only, not assistant responses
        return getUserMessageCount(userId, sessionId)
                .map(count -> count >= maxMessagesPerSession);
    }

    @Override
    public int getMaxMessagesPerSession() {
        return maxMessagesPerSession;
    }
}
