package com.example.javamate.factory;

import com.example.javamate.readerHandler.DocumentReader;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class DocumentReaderFactory {

    private final List<DocumentReader> readers;

    public DocumentReader getReader(String contentType) {

        return readers.stream()
                .filter(r -> r.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported document type: " + contentType));
    }
}
