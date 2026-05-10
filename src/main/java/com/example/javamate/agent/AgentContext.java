package com.example.javamate.agent;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Immutable request envelope passed to every agent.
 *
 * @param query     the (possibly refined) user question for this agent
 * @param userId    authenticated user id (for tenant isolation in RAG)
 * @param sessionId chat session id
 * @param history   prior conversation messages (already trimmed by ChatMemory)
 */
public record AgentContext(
        String query,
        Long userId,
        String sessionId,
        List<Message> history
) {
    public String conversationId() {
        return userId + ":" + sessionId;
    }

    public AgentContext withQuery(String newQuery) {
        return new AgentContext(newQuery, userId, sessionId, history);
    }
}

