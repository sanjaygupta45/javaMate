package com.example.javamate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Client-facing snapshot of a chat session, sourced from the
 * {@code chat_sessions} table (the aggregate root for a conversation).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionDTO {

    private String sessionId;
    private String title;
    private String status;
    private Long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}

