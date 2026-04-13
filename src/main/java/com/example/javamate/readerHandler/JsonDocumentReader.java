package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class JsonDocumentReader implements DocumentReader {

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("json");
    }

    @Override
    public DocumentReaderDTO read(InputStream inputStream) {
        try {
            String text = new String(inputStream.readAllBytes());

            DocumentReaderDTO dto = new DocumentReaderDTO();
            dto.setContent(text);

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON", e);
        }
    }
}
