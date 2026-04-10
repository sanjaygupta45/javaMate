package com.example.javamate.service.impl;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import com.example.javamate.service.AIService;
import com.example.javamate.service.JavaMateService;
import com.example.javamate.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.javamate.constants.AppConstants.*;

@Service
@RequiredArgsConstructor
public class JavaMateServiceImpl implements JavaMateService {

    private static final Logger logger =
            LoggerFactory.getLogger(JavaMateServiceImpl.class);

    private final AIService aiService;


    @Override
    public TextQueryResponseDTO processTextQuery(TextQueryRequestDTO textQueryRequestDTO) {

        if (textQueryRequestDTO == null || textQueryRequestDTO.getQuery().isBlank()) {
            return ResponseUtils.buildResponse(
                    new TextQueryResponseDTO(),
                    NOT_OK,
                    List.of(QUERY_EMPTY)
            );
        }

        String response = aiService.generateResponse(textQueryRequestDTO.getQuery());

        TextQueryResponseDTO responseDTO = new TextQueryResponseDTO();
        responseDTO.setChatResponse(response);
        responseDTO.setResponseStatus(OK);
        responseDTO.setMessages(List.of(QUERY_SUCCESS));

        return responseDTO;
    }
}