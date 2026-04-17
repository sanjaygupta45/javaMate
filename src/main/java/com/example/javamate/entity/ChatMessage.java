package com.example.javamate.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Table("chat_messages")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    @Id
    @Column("message_id")
    private Long messageId;

    @Column("user_id")
    private Long userId;

    @Column("session_id")
    private String sessionId;

    @Column("role")
    private String role; // USER, ASSISTANT, SYSTEM

    @Column("content")
    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;
}

