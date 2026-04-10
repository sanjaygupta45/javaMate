package com.example.javamate.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionResponse {
    private  LocalDateTime timestamp;
    private  int status;
    private String exception;
    private  String message;
    private  String path;
}
