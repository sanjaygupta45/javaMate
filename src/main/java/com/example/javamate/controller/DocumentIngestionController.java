package com.example.javamate.controller;

import com.example.javamate.dto.DocumentIngestionResultDTO;
import com.example.javamate.dto.DocumentMetaDataDTO;
import com.example.javamate.security.AuthenticatedUserContext;
import com.example.javamate.service.DocumentIngestionService;
import com.example.javamate.service.UserDocumentService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
@AllArgsConstructor
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;
    private final UserDocumentService userDocumentService;
    private final AuthenticatedUserContext authContext;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentIngestionResultDTO> upload(@RequestPart("file") FilePart file) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> documentIngestionService.ingest(file, userId));
    }

    @GetMapping
    public Flux<DocumentMetaDataDTO> getUserDocuments() {
        return authContext.getCurrentUserId()
                .flatMapMany(userDocumentService::getDocumentsByUserId)
                .map(this::toDocumentMetaDataDTO);
    }

    @GetMapping("/{documentId}")
    public Mono<DocumentMetaDataDTO> getDocumentById(@PathVariable Long documentId) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> userDocumentService.getDocumentByIdAndUserId(documentId, userId))
                .map(this::toDocumentMetaDataDTO);
    }

    @DeleteMapping("/{documentId}")
    public Mono<Void> deleteDocument(@PathVariable Long documentId) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> userDocumentService.deleteDocumentByIdAndUserId(documentId, userId));
    }

    private DocumentMetaDataDTO toDocumentMetaDataDTO(com.example.javamate.entity.UserDocument doc) {
        return DocumentMetaDataDTO.builder()
                .documentId(doc.getDocumentId() != null ? doc.getDocumentId().toString() : null)
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .size(doc.getFileSize() != null ? doc.getFileSize() : 0L)
                .uploadedAt(doc.getUploadedAt())
                .build();
    }

}
