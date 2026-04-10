package com.example.javamate.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DocumentIngestionResultDTO {

    private UUID documentId;

    private String fileName;

    private int totalChunks;

    private int storedVectors;
}
