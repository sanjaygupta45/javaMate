package com.example.javamate.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO implements Serializable {

    private Long messageId;
    private String sessionId;
    private String role; // USER, ASSISTANT, SYSTEM
    private String content;
    private LocalDateTime createdAt;


}

