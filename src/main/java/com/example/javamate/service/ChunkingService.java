package com.example.javamate.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface ChunkingService {

    List<Document> chunk(Document document);

}
