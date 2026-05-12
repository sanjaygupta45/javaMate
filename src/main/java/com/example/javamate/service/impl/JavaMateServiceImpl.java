package com.example.javamate.service.impl;

import com.example.javamate.agent.AgentName;
import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import com.example.javamate.dto.stream.AgentStreamEvent;
import com.example.javamate.exception.SessionLimitExceededException;
import com.example.javamate.service.AIService;
import com.example.javamate.service.ChatMemoryService;
import com.example.javamate.service.JavaMateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.javamate.constants.AppConstants.*;

/**
 * JavaMate service implementation.
 * 
 * Chat Memory is handled by Spring AI's MessageChatMemoryAdvisor:
 * - Messages are automatically saved to ChatMemory by the advisor
 * - History is automatically retrieved and added to prompts
 * 
 * This service only handles:
 * - Session limit checking (before allowing a query)
 * - Session ID resolution
 * - Error handling
 */
@Service
@RequiredArgsConstructor
public class JavaMateServiceImpl implements JavaMateService {

    private static final Logger logger =
            LoggerFactory.getLogger(JavaMateServiceImpl.class);

    private final AIService aiService;
    private final ChatMemoryService chatMemoryService;

    @Override
    public Mono<TextQueryResponseDTO> processTextQuery(TextQueryRequestDTO textQueryRequestDTO, Long userId) {

        if (textQueryRequestDTO == null || textQueryRequestDTO.getQuery() == null ||
            textQueryRequestDTO.getQuery().isBlank()) {

            TextQueryResponseDTO errorResponse = new TextQueryResponseDTO();
            errorResponse.setResponseStatus(NOT_OK);
            errorResponse.setMessages(List.of(QUERY_EMPTY));
            return Mono.just(errorResponse);
        }

        String sessionId = resolveSessionId(textQueryRequestDTO);
        String userQuery = textQueryRequestDTO.getQuery();

        return checkSessionLimit(userId, sessionId)
                .then(aiService.generateDetailedResponse(userQuery, userId, sessionId))
                .map(detailed -> {
                    TextQueryResponseDTO responseDTO = new TextQueryResponseDTO();
                    responseDTO.setChatResponse(detailed.answer());
                    responseDTO.setSessionId(sessionId);
                    responseDTO.setResponseStatus(OK);
                    responseDTO.setMessages(List.of(QUERY_SUCCESS));
                    // Multi-agent metadata
                    responseDTO.setAgents(detailed.agents() == null ? List.of()
                            : detailed.agents().stream().map(AgentName::name).toList());
                    responseDTO.setRouteReason(detailed.routeReason());
                    responseDTO.setSources(detailed.sources());
                    responseDTO.setCitations(detailed.citations());
                    return responseDTO;
                })
                .onErrorResume(SessionLimitExceededException.class, error -> {
                    logger.warn("Session limit exceeded for session {}: {}", sessionId, error.getMessage());
                    TextQueryResponseDTO errorResponse = new TextQueryResponseDTO();
                    errorResponse.setResponseStatus(NOT_OK);
                    errorResponse.setSessionId(sessionId);
                    errorResponse.setMessages(List.of(error.getMessage()));
                    return Mono.just(errorResponse);
                })
                .onErrorResume(error -> {
                    logger.error("Error processing query: {}", error.getMessage());
                    TextQueryResponseDTO errorResponse = new TextQueryResponseDTO();
                    errorResponse.setResponseStatus(NOT_OK);
                    errorResponse.setSessionId(sessionId);
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

        String sessionId = resolveSessionId(textQueryRequestDTO);
        String userQuery = textQueryRequestDTO.getQuery();

        return checkSessionLimit(userId, sessionId)
                .thenMany(aiService.generateStreamingResponse(userQuery, userId, sessionId))
                .onErrorResume(error -> {
                    logger.error("Error streaming response: {}", error.getMessage());
                    return Flux.error(error);
                });
    }

    @Override
    public Flux<AgentStreamEvent> processTextQueryStreamEvents(TextQueryRequestDTO textQueryRequestDTO, Long userId) {

        if (textQueryRequestDTO == null || textQueryRequestDTO.getQuery() == null ||
            textQueryRequestDTO.getQuery().isBlank()) {
            return Flux.error(new IllegalArgumentException(QUERY_EMPTY));
        }

        String sessionId = resolveSessionId(textQueryRequestDTO);
        String userQuery = textQueryRequestDTO.getQuery();

        return checkSessionLimit(userId, sessionId)
                .thenMany(aiService.generateStreamingEvents(userQuery, userId, sessionId))
                .onErrorResume(error -> {
                    logger.error("Error streaming events: {}", error.getMessage());
                    return Flux.error(error);
                });
    }


    //Check if session has reached the user message limit.
    private Mono<Void> checkSessionLimit(Long userId, String sessionId) {
        return chatMemoryService.isSessionLimitReached(userId, sessionId)
                .flatMap(limitReached -> {
                    if (limitReached) {
                        return Mono.error(new SessionLimitExceededException(
                                sessionId, chatMemoryService.getMaxMessagesPerSession()));
                    }
                    return Mono.empty();
                });
    }

    @Override
    public String resolveSessionId(TextQueryRequestDTO requestDTO) {
        if (requestDTO.getSessionId() != null && !requestDTO.getSessionId().isBlank()) {
            return requestDTO.getSessionId();
        }
        return chatMemoryService.generateSessionId();
    }
}

