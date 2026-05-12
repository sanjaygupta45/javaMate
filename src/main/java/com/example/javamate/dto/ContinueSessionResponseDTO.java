package com.example.javamate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContinueSessionResponseDTO {


    // new session id
    private String sessionId;

    private String previousSessionId;

    private String summary;
}

