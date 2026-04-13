package com.example.javamate.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long userId;
    private String name;
    private String email;
}

