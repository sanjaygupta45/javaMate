package com.example.javamate.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface VectorDatabaseService {

        void storeBatch(List<Document> documents);

        List<Document> similaritySearch(String query, int topK);

}
