package com.example.javamate.service;

import com.example.javamate.agent.orchestrator.OrchestratorResult;
import com.example.javamate.dto.stream.AgentStreamEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AIService {


    Mono<String> generateResponse(String userQuery, Long userId, String sessionId);

    /** Non-streaming with full agent metadata (route reason, sources, citations). */
    Mono<OrchestratorResult> generateDetailedResponse(String userQuery, Long userId, String sessionId);


    Flux<String> generateStreamingResponse(String userQuery, Long userId, String sessionId);

    /** Streaming with typed agent events instead of raw token strings. */
    Flux<AgentStreamEvent> generateStreamingEvents(String userQuery, Long userId, String sessionId);
}
