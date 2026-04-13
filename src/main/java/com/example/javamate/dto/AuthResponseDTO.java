package com.example.javamate.dto;

import lombok.*;

@Getter
@Setter
public class AuthResponseDTO {

    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
    private Long expiresIn; // in seconds


}

