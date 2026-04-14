package com.example.javamate.service;

import com.example.javamate.dto.DocumentIngestionResultDTO;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface DocumentIngestionService {

    Mono<DocumentIngestionResultDTO> ingest(FilePart filePart, Long userId);
}
