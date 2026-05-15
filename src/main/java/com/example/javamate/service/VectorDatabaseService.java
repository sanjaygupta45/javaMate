package com.example.javamate.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface VectorDatabaseService {

    void storeBatch(List<Document> documents);


    List<Document> similaritySearchByUserId(String query, int topK, Long userId);
}
