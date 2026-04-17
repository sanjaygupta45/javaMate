package com.example.javamate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a chat session summary.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionDTO {
    
    private String sessionId;
    private LocalDateTime lastMessageAt;
    private Long messageCount;
}

