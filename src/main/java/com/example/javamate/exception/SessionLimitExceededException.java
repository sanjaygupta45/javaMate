package com.example.javamate.exception;

import lombok.Getter;


@Getter
public class SessionLimitExceededException extends RuntimeException {

    private final String sessionId;
    private final int maxMessages;

    public SessionLimitExceededException(String sessionId, int maxMessages) {
        super(String.format(
                "This conversation has reached its %d-message limit. " +
                "Start a new conversation to continue chat ",
                maxMessages));
        this.sessionId = sessionId;
        this.maxMessages = maxMessages;
    }

}
