package com.example.javamate.repository;

import com.example.javamate.entity.UserDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserDocumentRepository extends ReactiveCrudRepository<UserDocument, Long> {

    Flux<UserDocument> findByUserId(Long userId);

    Mono<UserDocument> findByDocumentIdAndUserId(Long documentId, Long userId);
}

