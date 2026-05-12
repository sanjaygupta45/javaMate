package com.example.javamate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Table("chat_sessions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession implements Persistable<String> {

    @Id
    @Column("session_id")
    private String sessionId;

    @Column("user_id")
    private Long userId;

    @Column("title")
    private String title;


    @Column("status")
    private String status;

    @Column("message_count")
    private Integer messageCount;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("last_message_at")
    private LocalDateTime lastMessageAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;


    @Transient
    @Builder.Default
    private boolean newRecord = false;

    @Override
    public String getId() {
        return sessionId;
    }

    @Override
    public boolean isNew() {
        return newRecord;
    }

    public ChatSession markNew() {
        this.newRecord = true;
        return this;
    }
}

