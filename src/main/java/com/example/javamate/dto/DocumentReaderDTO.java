package com.example.javamate.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DocumentReaderDTO {
    private String content;

    private Map<String, Object> metadata;
}
