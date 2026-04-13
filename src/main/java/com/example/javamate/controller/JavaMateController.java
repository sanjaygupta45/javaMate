package com.example.javamate.controller;

import com.example.javamate.dto.ChatChunk;
import com.example.javamate.dto.TextQueryRequestDTO;
import com.example.javamate.dto.TextQueryResponseDTO;
import com.example.javamate.service.JavaMateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.example.javamate.constants.AppConstants.OK;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class JavaMateController {

    private final JavaMateService javaMateService;


     // Non-streaming endpoint - returns complete response
    @PostMapping(
            value = "/text",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<TextQueryResponseDTO>> askText(
            @RequestBody TextQueryRequestDTO requestDTO) {

        return javaMateService.processTextQuery(requestDTO)
                .map(response -> {
                    HttpStatus status = OK.equals(response.getResponseStatus()) 
                            ? HttpStatus.OK 
                            : HttpStatus.BAD_REQUEST;
                    return ResponseEntity.status(status).body(response);
                });
    }


    // streaming endpoints return Server-Sent events stream for real time experience
    // Using ChatChunk wrapper to preserve whitespace in JSON format
    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<ChatChunk>> askTextStream(@RequestBody TextQueryRequestDTO requestDTO) {
        return javaMateService.processTextQueryStream(requestDTO)
                .map(chunk -> ServerSentEvent.<ChatChunk>builder()
                        .data(new ChatChunk(chunk))
                        .build());
    }

    // if client prefer json data
    @PostMapping(
            value = "/stream/json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_NDJSON_VALUE
    )
    public Flux<ChatChunk> askTextStreamJson(@RequestBody TextQueryRequestDTO requestDTO) {
        return javaMateService.processTextQueryStream(requestDTO)
                .map(ChatChunk::new);
    }
    
    // New endpoint: Accumulates response and returns complete text (useful for debugging)
    @PostMapping(
            value = "/stream/accumulated",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<ChatChunk>> askTextStreamAccumulated(@RequestBody TextQueryRequestDTO requestDTO) {
        StringBuilder accumulated = new StringBuilder();
        return javaMateService.processTextQueryStream(requestDTO)
                .map(chunk -> {
                    accumulated.append(chunk);
                    return ServerSentEvent.<ChatChunk>builder()
                            .event("chunk")
                            .data(new ChatChunk(chunk))
                            .build();
                })
                .concatWith(Flux.defer(() -> Flux.just(
                        ServerSentEvent.<ChatChunk>builder()
                                .event("complete")
                                .data(new ChatChunk(accumulated.toString()))
                                .build()
                )));
    }

}