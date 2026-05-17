package com.example.javamate.controller;

import com.example.javamate.dto.AuthResponseDTO;
import com.example.javamate.dto.GoogleAuthRequestDTO;
import com.example.javamate.dto.LoginRequestDTO;
import com.example.javamate.dto.RegisterRequestDTO;
import com.example.javamate.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;


    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("Received registration request for email: {}", request.getEmail());

        return userService.register(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }


    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Received login request for email: {}", request.getEmail());

        return userService.login(request)
                .map(ResponseEntity::ok);
    }


    @PostMapping("/google")
    public Mono<ResponseEntity<AuthResponseDTO>> googleLogin(@Valid @RequestBody GoogleAuthRequestDTO request) {
        log.info("Received Google login request");

        return userService.loginWithGoogle(request.getIdToken())
                .map(ResponseEntity::ok);
    }
}

