package com.example.javamate.agent.orchestrator;

import com.example.javamate.agent.AgentName;
import com.example.javamate.dto.TextQueryResponseDTO.CitationRef;
import com.example.javamate.dto.TextQueryResponseDTO.SourceRef;

import java.util.List;

/**
 * Rich result produced by {@link AgentOrchestrator#handleDetailed} for the
 * non-streaming endpoint. Carries everything the controller needs to fill
 * {@link com.example.javamate.dto.TextQueryResponseDTO}.
 */
public record OrchestratorResult(
        String answer,
        List<AgentName> agents,
        String routeReason,
        List<SourceRef> sources,
        List<CitationRef> citations
) {}

