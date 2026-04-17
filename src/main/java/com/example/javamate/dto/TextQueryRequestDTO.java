package com.example.javamate.dto;


import lombok.Data;

@Data
public class TextQueryRequestDTO {
    private String query;

    private String sessionId;
}
