package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;

import java.io.InputStream;

public interface DocumentReader {

    DocumentReaderDTO read(InputStream inputStream);

    boolean supports(String contentType);

}
