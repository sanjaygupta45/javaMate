package com.example.javamate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContinueSessionRequestDTO {

    @NotBlank(message = "previousSessionId is required")
    private String previousSessionId;
}

