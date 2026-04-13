package com.example.javamate.service;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface JavaMateService {


    Mono<TextQueryResponseDTO> processTextQuery(TextQueryRequestDTO textQueryRequestDTO);


    Flux<String> processTextQueryStream(TextQueryRequestDTO textQueryRequestDTO);
}
