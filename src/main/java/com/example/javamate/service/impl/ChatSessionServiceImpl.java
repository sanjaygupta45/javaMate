package com.example.javamate.service.impl;

import com.example.javamate.entity.ChatSession;
import com.example.javamate.repository.ChatSessionRepository;
import com.example.javamate.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.example.javamate.constants.AppConstants.SESSION_STATUS_ACTIVE;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionServiceImpl.class);

    private static final int TITLE_MAX_LEN = 80;

    private final ChatSessionRepository chatSessionRepository;

    @Override
    public Mono<ChatSession> createSession(Long userId, String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status(SESSION_STATUS_ACTIVE)
                .messageCount(0)
                .createdAt(now)
                .lastMessageAt(now)
                .updatedAt(now)
                .build()
                .markNew();
        return chatSessionRepository.save(session)
                .doOnSuccess(s -> log.info("[Session] created userId={} sessionId={}", userId, sessionId))
                // If two requests race on the same brand-new session, fall back to fetching it.
                .onErrorResume(DuplicateKeyException.class,
                        e -> chatSessionRepository.findById(sessionId));
    }

    @Override
    public Mono<ChatSession> getOrCreate(Long userId, String sessionId) {
        return chatSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.defer(() -> createSession(userId, sessionId)));
    }

    @Override
    public Mono<Void> touch(String sessionId) {
        return chatSessionRepository.touchSession(sessionId, LocalDateTime.now())
                .doOnNext(rows -> {
                    if (rows == 0) {
                        log.warn("[Session] touch() found no row for sessionId={}", sessionId);
                    }
                })
                .then();
    }

    @Override
    public Mono<Void> setTitleIfBlank(String sessionId, String title) {
        if (title == null || title.isBlank()) return Mono.empty();
        String trimmed = title.length() > TITLE_MAX_LEN ? title.substring(0, TITLE_MAX_LEN) : title;
        return chatSessionRepository.updateTitleIfBlank(sessionId, trimmed, LocalDateTime.now()).then();
    }

    @Override
    public Mono<ChatSession> findForUser(Long userId, String sessionId) {
        return chatSessionRepository.findBySessionIdAndUserId(sessionId, userId);
    }

    @Override
    public Flux<ChatSession> listForUser(Long userId) {
        return chatSessionRepository.findByUserIdOrderByLastMessageAtDesc(userId);
    }

    @Override
    public Mono<Void> deleteForUser(Long userId, String sessionId) {
        return chatSessionRepository.deleteBySessionIdAndUserId(sessionId, userId)
                .doOnSuccess(v -> log.info("[Session] deleted userId={} sessionId={}", userId, sessionId));
    }

    @Override
    public Mono<Void> deleteAllForUser(Long userId) {
        return chatSessionRepository.deleteByUserId(userId)
                .doOnSuccess(v -> log.info("[Session] deleted all sessions for userId={}", userId));
    }
}

