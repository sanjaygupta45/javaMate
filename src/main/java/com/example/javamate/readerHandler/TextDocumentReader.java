package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class TextDocumentReader implements DocumentReader {

    @Override
    public boolean supports(String contentType) {
        return "text/plain".equals(contentType);
    }

    @Override
    public DocumentReaderDTO read(MultipartFile file) {
        try {

            String text = new String(file.getBytes());

            DocumentReaderDTO result = new DocumentReaderDTO();
            result.setContent(text);

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read text file", e);
        }
    }
}
