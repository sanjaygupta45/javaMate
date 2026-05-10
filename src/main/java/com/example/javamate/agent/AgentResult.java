package com.example.javamate.agent;

/**
 * Output of a single specialist agent invocation.
 */
public record AgentResult(AgentName agent, String content) {}

