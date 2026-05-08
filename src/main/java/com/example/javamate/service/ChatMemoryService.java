package com.example.javamate.service;

import com.example.javamate.dto.ChatSessionDTO;
import com.example.javamate.entity.ChatMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatMemoryService {


    Mono<ChatMessage> saveUserMessage(Long userId, String sessionId, String content);
    
    Mono<ChatMessage> saveAssistantMessage(Long userId, String sessionId, String content);
    
    Flux<ChatMessage> getRecentMessages(Long userId, String sessionId, int limit);
    
    Flux<ChatMessage> getSessionMessages(Long userId, String sessionId);
    
    Mono<Void> clearSession(Long userId, String sessionId);
    
    Mono<Void> clearAllUserSessions(Long userId);
    
    Flux<ChatSessionDTO> listUserSessions(Long userId);
    
    String generateSessionId();
    
    Mono<Long> getUserMessageCount(Long userId, String sessionId);
    
    Mono<Boolean> isSessionLimitReached(Long userId, String sessionId);

    int getMaxMessagesPerSession();

    // Compact memory: save summary message with SUMMARY role
    Mono<ChatMessage> saveSummaryMessage(Long userId, String sessionId, String summaryContent);

    // Get oldest messages (excluding summaries) for compaction
    Flux<ChatMessage> getOldestMessagesForCompaction(Long userId, String sessionId, int limit);

    // Delete messages by their IDs after compaction
    Mono<Void> deleteMessagesByIds(java.util.List<Long> messageIds);

    // Count non-summary messages to check if compaction is needed
    Mono<Long> getNonSummaryMessageCount(Long userId, String sessionId);
}
