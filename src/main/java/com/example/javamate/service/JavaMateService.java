package com.example.javamate.service;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface JavaMateService {

    /**
     * Process text query and return complete response
     */
    Mono<TextQueryResponseDTO> processTextQuery(TextQueryRequestDTO textQueryRequestDTO);

    /**
     * Process text query and stream response tokens
     */
    Flux<String> processTextQueryStream(TextQueryRequestDTO textQueryRequestDTO);
}
