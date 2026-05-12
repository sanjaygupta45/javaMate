package com.example.javamate.controller;

import com.example.javamate.dto.ChatMessageDTO;
import com.example.javamate.dto.ChatSessionDTO;
import com.example.javamate.dto.ContinueSessionRequestDTO;
import com.example.javamate.dto.ContinueSessionResponseDTO;
import com.example.javamate.dto.ResponseDTO;
import com.example.javamate.entity.ChatSession;
import com.example.javamate.security.AuthenticatedUserContext;
import com.example.javamate.service.ChatMemoryService;
import com.example.javamate.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.javamate.constants.AppConstants.OK;

// for managing chat session and chat history
@RestController
@RequestMapping("/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatMemoryService chatMemoryService;
    private final ChatSessionService chatSessionService;
    private final AuthenticatedUserContext authContext;


    @GetMapping
    public Mono<ResponseEntity<List<ChatSessionDTO>>> listSessions() {
        return authContext.getCurrentUserId()
                .flatMapMany(chatMemoryService::listUserSessions)
                .collectList()
                .map(ResponseEntity::ok);
    }

    // get all message by session id
    @GetMapping("/{sessionId}/messages")
    public Mono<ResponseEntity<List<ChatMessageDTO>>> getSessionMessages(
            @PathVariable String sessionId) {
        return authContext.getCurrentUserId()
                .flatMapMany(userId -> chatMemoryService.getSessionMessages(userId, sessionId))
                .map(this::toMessageDTO)
                .collectList()
                .map(ResponseEntity::ok);
    }

    private ChatMessageDTO toMessageDTO(com.example.javamate.entity.ChatMessage entity) {
        return ChatMessageDTO.builder()
                .messageId(entity.getMessageId())
                .sessionId(entity.getSessionId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }


    // Delete a specific chat session
    @DeleteMapping("/{sessionId}")
    public Mono<ResponseEntity<ResponseDTO>> deleteSession(
            @PathVariable String sessionId) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> chatMemoryService.clearSession(userId, sessionId))
                .then(Mono.fromCallable(() -> {
                    ResponseDTO response = new ResponseDTO();
                    response.setResponseStatus(OK);
                    response.setMessages(List.of("Session deleted successfully"));
                    return ResponseEntity.ok(response);
                }));
    }


     // Delete all chat sessions for the current user.
    @DeleteMapping
    public Mono<ResponseEntity<ResponseDTO>> deleteAllSessions() {
        return authContext.getCurrentUserId()
                .flatMap(chatMemoryService::clearAllUserSessions)
                .then(Mono.fromCallable(() -> {
                    ResponseDTO response = new ResponseDTO();
                    response.setResponseStatus(OK);
                    response.setMessages(List.of("All sessions deleted successfully"));
                    return ResponseEntity.ok(response);
                }));
    }


    //Generate a new session ID (useful for starting a new conversation).
    @PostMapping("/new")
    public Mono<ResponseEntity<ChatSessionDTO>> createNewSession() {
        return authContext.getCurrentUserId()
                .flatMap(userId -> {
                    String newSessionId = chatMemoryService.generateSessionId();
                    return chatSessionService.createSession(userId, newSessionId);
                })
                .map(this::toSessionDTO)
                .map(ResponseEntity::ok);
    }

    /** Monitor a single session (status, message count, activity). */
    @GetMapping("/{sessionId}")
    public Mono<ResponseEntity<ChatSessionDTO>> getSession(@PathVariable String sessionId) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> chatSessionService.findForUser(userId, sessionId))
                .map(this::toSessionDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private ChatSessionDTO toSessionDTO(ChatSession s) {
        return ChatSessionDTO.builder()
                .sessionId(s.getSessionId())
                .title(s.getTitle())
                .status(s.getStatus())
                .messageCount(s.getMessageCount() == null ? 0L : s.getMessageCount().longValue())
                .createdAt(s.getCreatedAt())
                .lastMessageAt(s.getLastMessageAt())
                .build();
    }

    @PostMapping(value = "/continue",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ContinueSessionResponseDTO>> continueSession(
            @Valid @RequestBody ContinueSessionRequestDTO request) {
        return authContext.getCurrentUserId()
                .flatMap(userId -> chatMemoryService.continueFromSession(userId, request.getPreviousSessionId()))
                .map(ResponseEntity::ok);
    }
}
