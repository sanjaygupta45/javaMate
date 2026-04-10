package com.example.javamate.service.impl;

import com.example.javamate.service.ChunkingService;
import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final TokenTextSplitter tokenTextSplitter;

    @Override
    public List<Document> chunk(Document document) {

        if (document == null || document.getText() == null || document.getText().isBlank()) {
            return List.of();
        }

        return tokenTextSplitter.split(List.of(document));
    }
}