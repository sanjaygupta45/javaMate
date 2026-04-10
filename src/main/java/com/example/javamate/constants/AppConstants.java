package com.example.javamate.constants;

public final class AppConstants {

    private AppConstants() {}

    // Response status
    public static final String OK = "OK";
    public static final String NOT_OK = "NOT_OK";

    // AI Model constants
    public static final String DEEPSEEK_CHAT_MODEL = "deepseek-chat";
    public static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";

    // Text query messages
    public static final String QUERY_EMPTY = "Query cannot be empty";
    public static final String QUERY_SUCCESS = "Query processed successfully";
}