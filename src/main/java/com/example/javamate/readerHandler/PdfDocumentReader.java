package com.example.javamate.readerHandler;

import com.example.javamate.dto.DocumentReaderDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfDocumentReader implements DocumentReader {

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }

    @Override
    public DocumentReaderDTO read(MultipartFile file) {

        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            DocumentReaderDTO result = new DocumentReaderDTO();
            result.setContent(text);

            return result;

        } catch (IOException ex) {
            throw new RuntimeException("Failed to read PDF", ex);
        }
    }
}