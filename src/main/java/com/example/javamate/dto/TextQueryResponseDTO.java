package com.example.javamate.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextQueryResponseDTO extends ResponseDTO {
    private String chatResponse;

    /**
     * Session ID for maintaining conversation context.
     * Client should send this back in subsequent requests to continue the conversation.
     */
    private String sessionId;

    // ---------------------------------------------------------------
    // Multi-agent metadata (filled by AgentOrchestrator.handleDetailed)
    // ---------------------------------------------------------------

    /** Specialist agents that produced this answer (1..2). */
    private List<String> agents;

    /** Short human-readable reason returned by the supervisor's router. */
    private String routeReason;

    /** Web sources used by WebSearchAgent (empty for other routes). */
    private List<SourceRef> sources;

    /** Personal-document citations used by PersonalRagAgent (empty for other routes). */
    private List<CitationRef> citations;

    /** Reference to a web search result surfaced by the WebSearch tool. */
    public record SourceRef(String title, String url, String snippet) {}

    /** Reference to a chunk in the user's personal knowledge base. */
    public record CitationRef(String documentId, String snippet) {}
}
