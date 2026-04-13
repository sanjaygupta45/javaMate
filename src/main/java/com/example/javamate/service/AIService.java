package com.example.javamate.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AIService {
    
    /**
     * Generate a complete response (blocking internally, wrapped in Mono)
     */
    Mono<String> generateResponse(String userQuery);

    /**
     * Generate streaming response - tokens are emitted as they arrive
     */
    Flux<String> generateStreamingResponse(String userQuery);
}
