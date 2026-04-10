package com.example.javamate.service;

import com.example.javamate.dto.DocumentIngestionResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIngestionService {

    DocumentIngestionResultDTO ingest(MultipartFile file);
}
