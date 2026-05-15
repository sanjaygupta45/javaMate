package com.example.javamate.repository;

import com.example.javamate.entity.ChatSession;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ChatSessionRepository extends ReactiveCrudRepository<ChatSession, String> {

    Flux<ChatSession> findByUserIdOrderByLastMessageAtDesc(Long userId);

    Mono<ChatSession> findBySessionIdAndUserId(String sessionId, Long userId);


    Mono<Void> deleteBySessionIdAndUserId(String sessionId, Long userId);

    Mono<Void> deleteByUserId(Long userId);

    /** Increment message_count and update activity timestamps in a single statement. */
    @Modifying
    @Query("""
            UPDATE chat_sessions
               SET message_count   = COALESCE(message_count, 0) + 1,
                   last_message_at = :ts,
                   updated_at      = :ts
             WHERE session_id = :sessionId
            """)
    Mono<Integer> touchSession(String sessionId, LocalDateTime ts);

    @Modifying
    @Query("UPDATE chat_sessions SET title = :title, updated_at = :ts WHERE session_id = :sessionId AND (title IS NULL OR title = '')")
    Mono<Integer> updateTitleIfBlank(String sessionId, String title, LocalDateTime ts);
}

