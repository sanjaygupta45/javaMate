package com.example.javamate.dto;

import com.example.javamate.agent.AgentName;
import com.example.javamate.dto.TextQueryResponseDTO.CitationRef;
import com.example.javamate.dto.TextQueryResponseDTO.SourceRef;

import java.util.List;


public record OrchestratorResult(
        String answer,
        List<AgentName> agents,
        String routeReason,
        List<SourceRef> sources,
        List<CitationRef> citations
) {}

