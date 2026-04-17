package com.example.javamate.exception;

import com.example.javamate.dto.ExceptionResponse;
import com.example.javamate.utils.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentLimitExceededException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleDocumentLimitExceeded(
            DocumentLimitExceededException ex,
            ServerWebExchange exchange) {

        log.warn("Document limit exceeded for user {}: {}", ex.getUserId(), ex.getMessage());

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                HttpStatus.BAD_REQUEST, "DOCUMENT_LIMIT_EXCEEDED", ex.getMessage(),
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(SessionLimitExceededException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleSessionLimitExceeded(
            SessionLimitExceededException ex,
            ServerWebExchange exchange) {

        log.warn("Session limit exceeded for session {}: {}", ex.getSessionId(), ex.getMessage());

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                HttpStatus.BAD_REQUEST, "SESSION_LIMIT_EXCEEDED", ex.getMessage(),
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(AppException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleAppException(
            AppException ex,
            ServerWebExchange exchange) {

        log.warn("{}: {}", ex.getErrorCode(), ex.getMessage());

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                ex, exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(ex.getHttpStatus()).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleValidationErrors(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errorMessage);

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errorMessage,
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleBadRequest(
            IllegalArgumentException ex,
            ServerWebExchange exchange) {

        log.warn("Bad request: {}", ex.getMessage());

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                ex, HttpStatus.BAD_REQUEST, exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleConflict(
            IllegalStateException ex,
            ServerWebExchange exchange) {

        log.warn("State conflict: {}", ex.getMessage());

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                ex, HttpStatus.CONFLICT, exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ExceptionResponse>> handleGeneral(
            Exception ex,
            ServerWebExchange exchange) {

        log.error("Unexpected error", ex);

        ExceptionResponse response = ResponseUtils.buildExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred",
                exchange.getRequest().getPath().value());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}