package com.example.javamate.factory;

import com.example.javamate.readerHandler.DocumentReader;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@Component
public class DocumentReaderFactory {

    private final List<DocumentReader> readers;

    public DocumentReader getReader(MultipartFile file) {

        return readers.stream()
                .filter(r -> r.supports(file.getContentType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported document"));
    }
}
