package com.example.javamate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public AppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = httpStatus.name();
    }

    public AppException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    // Common factory methods for convenience
    public static AppException notFound(String message) {
        return new AppException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static AppException conflict(String message) {
        return new AppException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static AppException unauthorized(String message) {
        return new AppException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static AppException badRequest(String message) {
        return new AppException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static AppException forbidden(String message) {
        return new AppException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static AppException internalError(String message) {
        return new AppException(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }
}

