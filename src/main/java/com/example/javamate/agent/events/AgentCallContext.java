package com.example.javamate.agent.events;

/**
 * Thread-local carrier for the current conversation id, so collaborators that
 * sit deep inside a blocking call (Spring AI tool loop, critic, ...) can publish
 * events on the {@link AgentEventBus} without having an {@code AgentContext}
 * threaded through their signatures.
 *
 * <p>An {@code Agent} that triggers tool calls (e.g. {@code WebSearchAgent})
 * MUST {@link #set(String)} before invoking the blocking chat call and
 * {@link #clear()} in a {@code finally}.
 */
public final class AgentCallContext {

    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();

    private AgentCallContext() { /* utility */ }

    public static void set(String conversationId) {
        CONVERSATION_ID.set(conversationId);
    }

    public static String get() {
        return CONVERSATION_ID.get();
    }

    public static void clear() {
        CONVERSATION_ID.remove();
    }
}

