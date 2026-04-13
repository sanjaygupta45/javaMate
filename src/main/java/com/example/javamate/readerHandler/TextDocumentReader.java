package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TextDocumentReader implements DocumentReader {

    @Override
    public boolean supports(String contentType) {
        return "text/plain".equals(contentType);
    }

    @Override
    public DocumentReaderDTO read(InputStream inputStream) {
        try {
            String text = new String(inputStream.readAllBytes());

            DocumentReaderDTO result = new DocumentReaderDTO();
            result.setContent(text);

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read text file", e);
        }
    }
}
