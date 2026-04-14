package com.example.javamate.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetaDataDTO {

    private String documentId;

    private String fileName;

    private String contentType;

    private long size;

    private LocalDateTime uploadedAt;

}
