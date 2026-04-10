package com.example.javamate.service;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;

public interface JavaMateService {


    TextQueryResponseDTO processTextQuery(TextQueryRequestDTO textQueryRequestDTO);

}
