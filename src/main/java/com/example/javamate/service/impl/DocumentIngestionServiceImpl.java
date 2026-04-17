package com.example.javamate.service.impl;

import com.example.javamate.dto.DocumentIngestionResultDTO;
import com.example.javamate.dto.DocumentReaderDTO;
import com.example.javamate.entity.UserDocument;
import com.example.javamate.exception.DocumentLimitExceededException;
import com.example.javamate.factory.DocumentReaderFactory;
import com.example.javamate.readerHandler.DocumentReader;
import com.example.javamate.repository.UserDocumentRepository;
import com.example.javamate.service.ChunkingService;
import com.example.javamate.service.DocumentIngestionService;
import com.example.javamate.service.VectorDatabaseService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final Logger logger =
            LoggerFactory.getLogger(DocumentIngestionServiceImpl.class);

    private final DocumentReaderFactory readerFactory;
    private final ChunkingService chunkingService;
    private final VectorDatabaseService vectorDatabaseService;
    private final UserDocumentRepository userDocumentRepository;

    @Value("${javamate.knowledge-base.max-documents-per-user:10}")
    private int maxDocumentsPerUser;

    @Override
    public Mono<DocumentIngestionResultDTO> ingest(FilePart filePart, Long userId) {

        // Check document limit before processing
        return checkDocumentLimit(userId)
                .then(DataBufferUtils.join(filePart.content())
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return bytes;
                        })
                        .flatMap(bytes -> processDocument(filePart, bytes, userId))
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    // check user is already reached his limit
    private Mono<Void> checkDocumentLimit(Long userId) {
        return userDocumentRepository.countByUserId(userId)
                .flatMap(count -> {
                    if (count >= maxDocumentsPerUser) {
                        logger.warn("User {} has reached the maximum document limit of {}", userId, maxDocumentsPerUser);
                        return Mono.error(new DocumentLimitExceededException(userId, maxDocumentsPerUser));
                    }
                    return Mono.empty();
                });
    }

    private Mono<DocumentIngestionResultDTO> processDocument(FilePart filePart, byte[] bytes, Long userId) {
        String fileName = filePart.filename();
        String contentType = filePart.headers().getContentType() != null 
                ? filePart.headers().getContentType().toString() 
                : "application/octet-stream";

        // Create UserDocument entity for database persistence (without documentId - will be auto-generated)
        UserDocument userDocument = UserDocument.builder()
                .userId(userId)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize((long) bytes.length)
                .totalChunks(0) // Will be updated after chunking
                .uploadedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save to DB first to get auto-generated documentId
        return userDocumentRepository.save(userDocument)
                .flatMap(savedDocument -> {
                    Long documentId = savedDocument.getDocumentId();
                    logger.info("Document saved with documentId: {} for userId: {}", documentId, userId);
                    
                    return Mono.fromCallable(() -> {
                        // Select appropriate reader
                        DocumentReader documentReader = readerFactory.getReader(contentType);

                        // Extract content using InputStream
                        InputStream inputStream = new ByteArrayInputStream(bytes);
                        DocumentReaderDTO documentReaderDTO = documentReader.read(inputStream);

                        String content = documentReaderDTO.getContent();

                        if (content == null || content.isBlank()) {
                            logger.info("Content not found");
                            throw new RuntimeException("Document content is empty");
                        }

                        // Create Spring AI document with userId in metadata
                        Document document = new Document(
                                content,
                                Map.of(
                                        "documentId", documentId.toString(),
                                        "fileName", Objects.requireNonNull(fileName),
                                        "userId", userId.toString()
                                )
                        );

                        // Chunk document
                        List<Document> chunks = chunkingService.chunk(document);

                        // Attach chunk metadata including userId
                        List<Document> enrichedChunks = new ArrayList<>();

                        for (int i = 0; i < chunks.size(); i++) {
                            Document chunk = chunks.get(i);

                            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                            metadata.put("documentId", documentId.toString());
                            metadata.put("chunkIndex", i);
                            metadata.put("fileName", fileName);
                            metadata.put("userId", userId.toString());

                            enrichedChunks.add(new Document(chunk.getText(), metadata));
                        }

                        // Store vectors
                        vectorDatabaseService.storeBatch(enrichedChunks);

                        // Build result DTO
                        DocumentIngestionResultDTO result = new DocumentIngestionResultDTO();
                        result.setDocumentId(documentId.toString());
                        result.setFileName(fileName);
                        result.setTotalChunks(enrichedChunks.size());
                        result.setStoredVectors(enrichedChunks.size());

                        return new ChunkingResult(result, enrichedChunks.size());
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(chunkingResult -> {
                        // Update totalChunks in DB
                        savedDocument.setTotalChunks(chunkingResult.totalChunks());
                        savedDocument.setUpdatedAt(LocalDateTime.now());
                        return userDocumentRepository.save(savedDocument)
                                .map(updated -> {
                                    logger.info("Document ingestion complete! documentId: {}, totalChunks: {}", 
                                            documentId, chunkingResult.totalChunks());
                                    return chunkingResult.result();
                                });
                    });
                });
    }

    private record ChunkingResult(DocumentIngestionResultDTO result, int totalChunks) {}
}
