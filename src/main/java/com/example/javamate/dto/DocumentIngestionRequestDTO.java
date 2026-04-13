package com.example.javamate.dto;


import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;

@Data
public class DocumentIngestionRequestDTO {
    private FilePart file;
}
