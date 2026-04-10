package com.example.javamate.utils;

import com.example.javamate.dto.ResponseDTO;

import java.util.List;

public final class ResponseUtils {

    // Private constructor to prevent instantiation
    private ResponseUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    public static <T extends ResponseDTO> T buildResponse(T responseDTO, String status, List<String> message) {
        responseDTO.setResponseStatus(status);
        responseDTO.setMessages(message);
        return responseDTO;
    }
}
