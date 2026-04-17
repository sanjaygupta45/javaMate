package com.example.javamate.exception;

import lombok.Getter;


@Getter
public class SessionLimitExceededException extends RuntimeException {

    private final String sessionId;
    private final int maxMessages;

    public SessionLimitExceededException(String sessionId, int maxMessages) {
        super(String.format("Session '%s' has reached the maximum limit of %d messages. Please start a new session.", 
                sessionId, maxMessages));
        this.sessionId = sessionId;
        this.maxMessages = maxMessages;
    }

}

