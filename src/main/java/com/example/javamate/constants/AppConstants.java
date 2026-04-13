package com.example.javamate.constants;

public final class AppConstants {

    private AppConstants() {}

    // Response status
    public static final String OK = "OK";
    public static final String NOT_OK = "NOT_OK";

    // AI Model constants
    public static final String MISTRAL_CHAT_MODEL = "mistral-large-latest";
    public static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";

    // Text query messages
    public static final String QUERY_EMPTY = "Query cannot be empty";
    public static final String QUERY_SUCCESS = "Query processed successfully";

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
}