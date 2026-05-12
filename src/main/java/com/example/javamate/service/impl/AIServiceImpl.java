package com.example.javamate.service.impl;

import com.example.javamate.agent.orchestrator.AgentOrchestrator;
import com.example.javamate.dto.OrchestratorResult;
import com.example.javamate.agent.events.AgentStreamEvent;
import com.example.javamate.service.AIService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Thin facade over the multi-agent {@link AgentOrchestrator}.
 *
 * <p>All routing, retrieval, web search and synthesis live in the {@code agent} package.
 * This class exists only to satisfy the existing {@link AIService} contract used by
 * controllers and {@link com.example.javamate.service.JavaMateService}.
 */
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private final AgentOrchestrator orchestrator;

    @Override
    public Mono<String> generateResponse(String userQuery, Long userId, String sessionId) {
        log.debug("[AIService] non-streaming request userId={} session={}", userId, sessionId);
        return orchestrator.handle(userQuery, userId, sessionId);
    }

    @Override
    public Mono<OrchestratorResult> generateDetailedResponse(String userQuery, Long userId, String sessionId) {
        log.debug("[AIService] non-streaming (detailed) userId={} session={}", userId, sessionId);
        return orchestrator.handleDetailed(userQuery, userId, sessionId);
    }

    @Override
    public Flux<String> generateStreamingResponse(String userQuery, Long userId, String sessionId) {
        log.debug("[AIService] streaming request userId={} session={}", userId, sessionId);
        return orchestrator.handleStream(userQuery, userId, sessionId);
    }

    @Override
    public Flux<AgentStreamEvent> generateStreamingEvents(String userQuery, Long userId, String sessionId) {
        log.debug("[AIService] streaming (typed events) userId={} session={}", userId, sessionId);
        return orchestrator.handleStreamEvents(userQuery, userId, sessionId);
    }
}
