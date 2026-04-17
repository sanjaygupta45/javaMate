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
}
