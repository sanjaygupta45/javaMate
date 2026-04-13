package com.example.javamate.controller;

import com.example.javamate.dto.DocumentIngestionResultDTO;
import com.example.javamate.service.DocumentIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
@AllArgsConstructor
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentIngestionResultDTO> upload(@RequestPart("file") FilePart file) {
        return documentIngestionService.ingest(file);
    }
}
