package com.example.javamate.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextQueryResponseDTO extends ResponseDTO {
    private String chatResponse;

    private String sessionId;

    private String sessionTitle;

    private List<SourceRef> sources;

    public record SourceRef(String title, String url, String snippet) {}

    public record CitationRef(String documentId, String snippet) {}
}
