package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentReader {

    DocumentReaderDTO read(MultipartFile file);

    boolean supports(String contentType);

}
