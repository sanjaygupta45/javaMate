package com.example.javamate.agent.events;

import com.example.javamate.agent.AgentName;

import java.util.List;
import java.util.Map;

public sealed interface AgentStreamEvent
        permits AgentStreamEvent.RouteEvent,
                AgentStreamEvent.ToolEvent,
                AgentStreamEvent.TokenEvent,
                AgentStreamEvent.CritiqueEvent,
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
}

