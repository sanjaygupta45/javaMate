package com.example.javamate.agent.router;

import com.example.javamate.agent.AgentName;

import java.util.List;

/**
 * Structured output produced by the SupervisorAgent's router LLM call.
 *
 * @param agents       ordered list of specialist agents to invoke (1..N)
 * @param refinedQuery the query rewritten / clarified for the specialists
 * @param reason       short justification (useful for logging / debugging)
 */
public record RouteDecision(
        List<AgentName> agents,
        String refinedQuery,
        String reason
) {}
