package com.example.javamate.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private Long expiresIn; // in seconds

    public static AuthResponseDTO of(String token, Long userId, String name, String email, Long expiresInSeconds) {
        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userId)
                .name(name)
                .email(email)
                .expiresIn(expiresInSeconds)
                .build();
    }
}

