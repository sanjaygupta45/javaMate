package com.example.javamate.utils;

import com.example.javamate.dto.ExceptionResponse;
import com.example.javamate.exception.AppException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public final class ResponseUtils {

    // Private constructor to prevent instantiation
    private ResponseUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }


    public static ExceptionResponse buildExceptionResponse(AppException ex, String path) {
        return ExceptionResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .exception(ex.getErrorCode())
                .message(ex.getMessage())
                .path(path)
                .build();
    }

    public static ExceptionResponse buildExceptionResponse(Exception ex, HttpStatus status, String path) {
        return ExceptionResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .exception(ex.getClass().getSimpleName())
                .message(ex.getMessage())
                .path(path)
                .build();
    }

    public static ExceptionResponse buildExceptionResponse(HttpStatus status, String errorCode, String message, String path) {
        return ExceptionResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .exception(errorCode)
                .message(message)
                .path(path)
                .build();
    }
}
