package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class TikaDocumentReader implements DocumentReader {
    private final Tika tika = new Tika();

    @Override
    public boolean supports(String contentType) {
        return true; // fallback reader
    }

    @Override
    public DocumentReaderDTO read(InputStream inputStream) {

        try {
            String text = tika.parseToString(inputStream);

            DocumentReaderDTO documentDTO = new DocumentReaderDTO();
            documentDTO.setContent(text);

            return documentDTO;

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract document", e);
        }
    }

}
