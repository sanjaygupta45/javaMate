package com.example.javamate.exception;

import lombok.Getter;


@Getter
public class DocumentLimitExceededException extends RuntimeException {

    private final Long userId;
    private final int maxDocuments;

    public DocumentLimitExceededException(Long userId, int maxDocuments) {
        super(String.format("User has reached the maximum limit of %d documents in the knowledge base. Please delete some documents before uploading new ones.", 
                maxDocuments));
        this.userId = userId;
        this.maxDocuments = maxDocuments;
    }

}

