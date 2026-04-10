package com.example.javamate.controller;

import com.example.javamate.dto.DocumentIngestionRequestDTO;
import com.example.javamate.dto.DocumentIngestionResultDTO;
import com.example.javamate.service.DocumentIngestionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
@AllArgsConstructor
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data" )
    public DocumentIngestionResultDTO upload(@ModelAttribute  DocumentIngestionRequestDTO documentIngestionRequestDTO) {
        return documentIngestionService.ingest(documentIngestionRequestDTO.getFile());

    }
}
