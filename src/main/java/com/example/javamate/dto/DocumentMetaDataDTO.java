package com.example.javamate.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetaDataDTO {
    private UUID documentId;

    private String fileName;

    private String contentType;

    private long size;

    private LocalDateTime uploadedAt;

}
