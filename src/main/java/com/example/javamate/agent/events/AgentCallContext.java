package com.example.javamate.agent.events;


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

