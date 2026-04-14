package com.example.javamate.dto;

import lombok.Data;

@Data
public class DocumentIngestionResultDTO {

    private String documentId;

    private String fileName;

    private int totalChunks;

    private int storedVectors;
}
