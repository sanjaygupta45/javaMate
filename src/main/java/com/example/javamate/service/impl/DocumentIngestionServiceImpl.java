package com.example.javamate.service.impl;

import com.example.javamate.dto.DocumentMetaDataDTO;
import com.example.javamate.dto.DocumentIngestionResultDTO;
import com.example.javamate.dto.DocumentReaderDTO;
import com.example.javamate.factory.DocumentReaderFactory;
import com.example.javamate.readerHandler.DocumentReader;
import com.example.javamate.service.ChunkingService;
import com.example.javamate.service.DocumentIngestionService;
import com.example.javamate.service.VectorDatabaseService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
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

    @Override
    public Mono<DocumentIngestionResultDTO> ingest(FilePart filePart) {
        
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> processDocument(filePart, bytes))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<DocumentIngestionResultDTO> processDocument(FilePart filePart, byte[] bytes) {
        return Mono.fromCallable(() -> {
            UUID documentId = UUID.randomUUID();
            String fileName = filePart.filename();
            String contentType = filePart.headers().getContentType() != null 
                    ? filePart.headers().getContentType().toString() 
                    : "application/octet-stream";

            // Create metadata DTO
            DocumentMetaDataDTO documentMetaDataDTO = DocumentMetaDataDTO.builder()
                    .documentId(documentId)
                    .fileName(fileName)
                    .contentType(contentType)
                    .size((long) bytes.length)
                    .uploadedAt(LocalDateTime.now())
                    .build();

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

            // Create Spring AI document
            Document document = new Document(
                    content,
                    Map.of(
                            "documentId", documentId.toString(),
                            "fileName", Objects.requireNonNull(fileName)
                    )
            );

            // Chunk document
            List<Document> chunks = chunkingService.chunk(document);

            // Attach chunk metadata
            List<Document> enrichedChunks = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);

                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("documentId", documentId.toString());
                metadata.put("chunkIndex", i);
                metadata.put("fileName", fileName);

                enrichedChunks.add(new Document(chunk.getText(), metadata));
            }

            // Store vectors
            vectorDatabaseService.storeBatch(enrichedChunks);

            // Build result DTO
            DocumentIngestionResultDTO result = new DocumentIngestionResultDTO();
            result.setDocumentId(documentMetaDataDTO.getDocumentId());
            result.setFileName(documentMetaDataDTO.getFileName());
            result.setTotalChunks(enrichedChunks.size());
            result.setStoredVectors(enrichedChunks.size());

            return result;
        });
    }
}
