package com.example.javamate.agent.events;

import com.example.javamate.agent.AgentName;

import java.util.List;
import java.util.Map;

public sealed interface AgentStreamEvent
        permits AgentStreamEvent.RouteEvent,
                AgentStreamEvent.ToolEvent,
                AgentStreamEvent.TokenEvent,
                AgentStreamEvent.CritiqueEvent,
                AgentStreamEvent.SessionEvent,
                AgentStreamEvent.ErrorEvent,
                AgentStreamEvent.DoneEvent {


    String type();

    record RouteEvent(List<AgentName> agents, String reason, String refinedQuery)
            implements AgentStreamEvent {
        @Override public String type() { return "route"; }
    }

    record ToolEvent(String name, Map<String, Object> args, Object results)
            implements AgentStreamEvent {
        @Override public String type() { return "tool"; }
    }

    record TokenEvent(String content) implements AgentStreamEvent {
        @Override public String type() { return "token"; }
    }

    record CritiqueEvent(boolean approved, List<String> issues, String suggestions, int iteration)
            implements AgentStreamEvent {
        @Override public String type() { return "critique"; }
    }

    record DoneEvent(String sessionId, long totalTokens) implements AgentStreamEvent {
        @Override public String type() { return "done"; }
    }

    /**
     * Emitted once per stream, after the user message has been persisted and the
     * session row (with derived title) is available. Lets the client render the
     * conversation title without polling /chat/sessions/{id}.
     */
    record SessionEvent(String sessionId, String title) implements AgentStreamEvent {
        @Override public String type() { return "session"; }
    }

    /**
     * Terminal structured error. Emitted instead of (or before) DoneEvent when
     * the pipeline fails (session-limit reached, LLM error, persistence error, ...).
     */
    record ErrorEvent(String code, String message) implements AgentStreamEvent {
        @Override public String type() { return "error"; }
    }
}
