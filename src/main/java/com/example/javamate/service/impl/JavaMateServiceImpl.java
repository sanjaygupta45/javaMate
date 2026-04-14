package com.example.javamate.service.impl;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import com.example.javamate.service.AIService;
import com.example.javamate.service.JavaMateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.javamate.constants.AppConstants.*;

@Service
@RequiredArgsConstructor
public class JavaMateServiceImpl implements JavaMateService {

    private static final Logger logger =
            LoggerFactory.getLogger(JavaMateServiceImpl.class);

    private final AIService aiService;

    @Override
    public Mono<TextQueryResponseDTO> processTextQuery(TextQueryRequestDTO textQueryRequestDTO, Long userId) {

        if (textQueryRequestDTO == null || textQueryRequestDTO.getQuery() == null || 
            textQueryRequestDTO.getQuery().isBlank()) {
            
            TextQueryResponseDTO errorResponse = new TextQueryResponseDTO();
            errorResponse.setResponseStatus(NOT_OK);
            errorResponse.setMessages(List.of(QUERY_EMPTY));
            return Mono.just(errorResponse);
        }

        return aiService.generateResponse(textQueryRequestDTO.getQuery(), userId)
                .map(response -> {
                    TextQueryResponseDTO responseDTO = new TextQueryResponseDTO();
                    responseDTO.setChatResponse(response);
                    responseDTO.setResponseStatus(OK);
                    responseDTO.setMessages(List.of(QUERY_SUCCESS));
                    return responseDTO;
                })
                .onErrorResume(error -> {
                    logger.error("Error processing query: {}", error.getMessage());
                    TextQueryResponseDTO errorResponse = new TextQueryResponseDTO();
                    errorResponse.setResponseStatus(NOT_OK);
                    errorResponse.setMessages(List.of("Error processing query: " + error.getMessage()));
                    return Mono.just(errorResponse);
                });
    }

    @Override
    public Flux<String> processTextQueryStream(TextQueryRequestDTO textQueryRequestDTO, Long userId) {

        if (textQueryRequestDTO == null || textQueryRequestDTO.getQuery() == null || 
            textQueryRequestDTO.getQuery().isBlank()) {
            return Flux.error(new IllegalArgumentException(QUERY_EMPTY));
        }

        return aiService.generateStreamingResponse(textQueryRequestDTO.getQuery(), userId)
                .onErrorResume(error -> {
                    logger.error("Error streaming response: {}", error.getMessage());
                    return Flux.error(error);
                });
    }
}