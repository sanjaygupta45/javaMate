package com.example.javamate.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScrapedPage {
    private String url;
    private int pageNumber;
    private boolean isUnicode;
    private List<Section> sections;

    @Data
    @Builder
    public static class Section {
        private String heading;
        private String body;
    }
}