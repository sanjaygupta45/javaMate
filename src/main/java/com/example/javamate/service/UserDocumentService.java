package com.example.javamate.service;

import com.example.javamate.entity.UserDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserDocumentService {

    Flux<UserDocument> getDocumentsByUserId(Long userId);

    Mono<UserDocument> getDocumentByIdAndUserId(Long documentId, Long userId);

    Mono<Void> deleteDocumentByIdAndUserId(Long documentId, Long userId);
}

