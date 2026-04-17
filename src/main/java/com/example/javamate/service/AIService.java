package com.example.javamate.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AIService {


    Mono<String> generateResponse(String userQuery, Long userId, String sessionId);


    Flux<String> generateStreamingResponse(String userQuery, Long userId, String sessionId);
}
