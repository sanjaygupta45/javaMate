package com.example.javamate.dto;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentIngestionRequestDTO {
    private MultipartFile file;
}
