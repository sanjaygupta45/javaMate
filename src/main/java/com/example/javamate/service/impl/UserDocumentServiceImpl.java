package com.example.javamate.service.impl;

import com.example.javamate.entity.UserDocument;
import com.example.javamate.exception.AppException;
import com.example.javamate.repository.UserDocumentRepository;
import com.example.javamate.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDocumentServiceImpl implements UserDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(UserDocumentServiceImpl.class);

    private final UserDocumentRepository userDocumentRepository;

    @Override
    public Flux<UserDocument> getDocumentsByUserId(Long userId) {
        logger.info("Fetching documents for userId: {}", userId);
        return userDocumentRepository.findByUserId(userId);
    }

    @Override
    public Mono<UserDocument> getDocumentByIdAndUserId(Long documentId, Long userId) {
        logger.info("Fetching document: {} for userId: {}", documentId, userId);
        return userDocumentRepository.findByDocumentIdAndUserId(documentId, userId)
                .switchIfEmpty(Mono.error(AppException.notFound(
                        "Document not found with id: " + documentId)));
    }

    @Override
    public Mono<Void> deleteDocumentByIdAndUserId(Long documentId, Long userId) {
        logger.info("Deleting document: {} for userId: {}", documentId, userId);
        return userDocumentRepository.findByDocumentIdAndUserId(documentId, userId)
                .switchIfEmpty(Mono.error(AppException.notFound(
                        "Document not found with id: " + documentId)))
                .flatMap(userDocumentRepository::delete);
    }

}

