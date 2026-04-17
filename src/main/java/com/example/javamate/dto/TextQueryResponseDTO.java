package com.example.javamate.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextQueryResponseDTO extends ResponseDTO{
    private String chatResponse;
    
    /**
     * Session ID for maintaining conversation context.
     * Client should send this back in subsequent requests to continue the conversation.
     */
    private String sessionId;
}
