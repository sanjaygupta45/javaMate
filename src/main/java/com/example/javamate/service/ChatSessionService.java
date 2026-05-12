package com.example.javamate.service;

import com.example.javamate.entity.ChatSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ChatSessionService {


    Mono<ChatSession> createSession(Long userId, String sessionId);

    Mono<ChatSession> getOrCreate(Long userId, String sessionId);

    Mono<Void> touch(String sessionId);

    Mono<Void> setTitleIfBlank(String sessionId, String title);

    Mono<ChatSession> findForUser(Long userId, String sessionId);

    Flux<ChatSession> listForUser(Long userId);

    Mono<Void> deleteForUser(Long userId, String sessionId);

    Mono<Void> deleteAllForUser(Long userId);
}

