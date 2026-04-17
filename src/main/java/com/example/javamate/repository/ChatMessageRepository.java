package com.example.javamate.repository;

import com.example.javamate.entity.ChatMessage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, Long> {

    @Query("SELECT * FROM chat_messages WHERE user_id = :userId AND session_id = :sessionId ORDER BY created_at ASC LIMIT :limit")
    Flux<ChatMessage> findRecentMessages(Long userId, String sessionId, int limit);

    Flux<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);

    Mono<Void> deleteByUserIdAndSessionId(Long userId, String sessionId);

    Mono<Void> deleteByUserId(Long userId);

    @Query("SELECT DISTINCT session_id FROM chat_messages WHERE user_id = :userId")
    Flux<String> findDistinctSessionIdsByUserId(Long userId);

    Mono<Long> countByUserIdAndSessionId(Long userId, String sessionId);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE user_id = :userId AND session_id = :sessionId AND role = 'USER'")
    Mono<Long> countUserMessagesByUserIdAndSessionId(Long userId, String sessionId);

    @Query("SELECT * FROM chat_messages WHERE user_id = :userId AND session_id = :sessionId ORDER BY created_at DESC LIMIT 1")
    Mono<ChatMessage> findLatestMessageInSession(Long userId, String sessionId);
}




