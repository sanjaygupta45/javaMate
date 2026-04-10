package com.example.javamate.controller;

import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import com.example.javamate.service.JavaMateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.javamate.constants.AppConstants.OK;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class JavaMateController {

    private final JavaMateService javaMateService;


    @PostMapping(
            value = "/text",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TextQueryResponseDTO> askText(
            @RequestBody TextQueryRequestDTO requestDTO) {

        TextQueryResponseDTO textQueryResponseDTO =
                javaMateService.processTextQuery(requestDTO);

        return new ResponseEntity<>(textQueryResponseDTO,
                textQueryResponseDTO.getResponseStatus().equals(OK) ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}