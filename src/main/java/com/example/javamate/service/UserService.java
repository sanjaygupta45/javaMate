package com.example.javamate.service;

import com.example.javamate.dto.AuthResponseDTO;
import com.example.javamate.dto.LoginRequestDTO;
import com.example.javamate.dto.RegisterRequestDTO;
import com.example.javamate.entity.User;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<AuthResponseDTO> register(RegisterRequestDTO request);

    Mono<AuthResponseDTO> login(LoginRequestDTO request);

    Mono<User> findByEmail(String email);

    Mono<User> findById(Long userId);
}

