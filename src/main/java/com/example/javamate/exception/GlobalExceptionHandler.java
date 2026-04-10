package com.example.javamate.exception;

import com.example.javamate.dto.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final LocalDateTime currentTime = LocalDateTime.now();

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Bad request: {}", ex.getMessage());

        ExceptionResponse response = new ExceptionResponse(
                currentTime,
                HttpStatus.BAD_REQUEST.value(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponse> handleConflict(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.warn("State conflict: {}", ex.getMessage());

        ExceptionResponse response = new ExceptionResponse(
                currentTime,
                HttpStatus.CONFLICT.value(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ExceptionResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        ExceptionResponse response = new ExceptionResponse(
                currentTime,
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                ex.getClass().getSimpleName(),
                "Upload exceeds max allowed size",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error", ex);

        ExceptionResponse response = new ExceptionResponse(
                currentTime,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getClass().getSimpleName(),
                "An unexpected error occurred",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}