package com.example.javamate.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextQueryResponseDTO extends ResponseDTO{
    private String chatResponse;
}
