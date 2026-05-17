package com.example.javamate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequestDTO {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
