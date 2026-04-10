package com.example.javamate.service.impl;

import com.example.javamate.service.VectorDatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorDatabaseServiceImpl implements VectorDatabaseService {

    private final VectorStore vectorStore;

    @Override
    public void storeBatch(List<Document> documents) {
        vectorStore.add(documents);
    }

    @Override
    public List<Document> similaritySearch(String query, int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        return vectorStore.similaritySearch(request);
    }
}