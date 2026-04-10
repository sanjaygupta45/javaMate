package com.example.javamate.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ResponseDTO implements Serializable {
    private String responseStatus;
    private List<String> messages;
}
