package com.example.javamate.service.impl;

import com.example.javamate.dto.ChatSessionDTO;
import com.example.javamate.dto.ContinueSessionResponseDTO;
import com.example.javamate.entity.ChatMessage;
import com.example.javamate.repository.ChatMessageRepository;
import com.example.javamate.service.ChatMemoryService;
import com.example.javamate.service.ConversationSummarizer;
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
    private static final String ROLE_SUMMARY = "SUMMARY"; // Role for storing compacted conversation summaries

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationSummarizer conversationSummarizer;

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

    @Override
    public Mono<ChatMessage> saveSummaryMessage(Long userId, String sessionId, String summaryContent) {
        // Store summarized conversation context with SUMMARY role
        return saveMessage(userId, sessionId, ROLE_SUMMARY, summaryContent)
                .doOnSuccess(saved -> logger.info("Saved summary message for session {}", sessionId));
    }

    @Override
    public Flux<ChatMessage> getOldestMessagesForCompaction(Long userId, String sessionId, int limit) {
        // Fetch oldest messages (excluding existing summaries) for compaction
        return chatMessageRepository.findOldestNonSummaryMessages(userId, sessionId, limit);
    }

    @Override
    public Mono<Void> deleteMessagesByIds(java.util.List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Mono.empty();
        }
        return chatMessageRepository.deleteByMessageIds(messageIds)
                .doOnSuccess(v -> logger.debug("Deleted {} messages after compaction", messageIds.size()));
    }

    @Override
    public Mono<Long> getNonSummaryMessageCount(Long userId, String sessionId) {
        // Count messages excluding SUMMARY role to determine if compaction is needed
        return chatMessageRepository.countNonSummaryMessages(userId, sessionId);
    }

    @Override
    public Mono<ContinueSessionResponseDTO> continueFromSession(Long userId, String previousSessionId) {
        if (previousSessionId == null || previousSessionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("previousSessionId is required"));
        }

        return getSessionMessages(userId, previousSessionId)
                .collectList()
                .flatMap(messages -> {
                    if (messages.isEmpty()) {
                        return Mono.error(new IllegalArgumentException(
                                "Session '" + previousSessionId + "' is empty or not owned by this user."));
                    }
                    String newSessionId = generateSessionId();
                    return conversationSummarizer.summarise(messages)
                            .flatMap(summary -> saveSummaryMessage(userId, newSessionId, summary)
                                    .thenReturn(ContinueSessionResponseDTO.builder()
                                            .sessionId(newSessionId)
                                            .previousSessionId(previousSessionId)
                                            .summary(summary)
                                            .build()))
                            .doOnSuccess(r -> logger.info(
                                    "Continued user={} from session={} -> new session={}",
                                    userId, previousSessionId, r.getSessionId()));
                });
    }
}
