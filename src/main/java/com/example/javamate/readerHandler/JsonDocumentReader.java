package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class JsonDocumentReader implements DocumentReader{

    @Override
    public boolean supports(String contentType) {
        return contentType.contains("json");
    }

    @Override
    public DocumentReaderDTO read(MultipartFile file) {


        try {
            String text = new String(file.getBytes());

            DocumentReaderDTO dto = new DocumentReaderDTO();
            dto.setContent(text);

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON", e);
        }
    }
}
