package com.example.javamate.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("user_documents")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDocument {

    @Id
    @Column("document_id")
    private Long documentId;

    @Column("user_id")
    private Long userId;

    @Column("file_name")
    private String fileName;

    @Column("content_type")
    private String contentType;

    @Column("file_size")
    private Long fileSize;

    @Column("total_chunks")
    private Integer totalChunks;

    @Column("uploaded_at")
    private LocalDateTime uploadedAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

