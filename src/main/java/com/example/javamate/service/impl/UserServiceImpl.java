package com.example.javamate.service.impl;

import com.example.javamate.dto.AuthResponseDTO;
import com.example.javamate.dto.LoginRequestDTO;
import com.example.javamate.dto.RegisterRequestDTO;
import com.example.javamate.entity.User;
import com.example.javamate.exception.AppException;
import com.example.javamate.repository.UserRepository;
import com.example.javamate.service.JwtService;
import com.example.javamate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public Mono<AuthResponseDTO> register(RegisterRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();
        String namePart = request.getEmail().split("@")[0].toLowerCase().trim();
        String name = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);

        log.info("Attempting to register user with email: {}", email);

        return userRepository.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Registration failed - user already exists with email: {}", email);
                        return Mono.error(AppException.conflict(
                                "User with email '" + email + "' already exists"));
                    }

                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return userRepository.save(newUser);
                })
                .map(savedUser -> {
                    log.info("User registered successfully with ID: {}", savedUser.getUserId());
                    String token = jwtService.generateToken(savedUser);
                    return buildAuthResponseDTO(savedUser, token);
                });
    }

    @Override
    public Mono<AuthResponseDTO> login(LoginRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();

        log.info("Attempting to login user with email: {}", email);

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(AppException.notFound(
                        "User with email '" + email + "' not found. Please register first.")))
                .map(user -> {
                    log.info("User logged in successfully: {}", user.getUserId());
                    String token = jwtService.generateToken(user);
                    return buildAuthResponseDTO(user, token);
                });
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    @Override
    public Mono<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    private AuthResponseDTO buildAuthResponseDTO(User user, String token) {
        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setToken(token);
        authResponseDTO.setUserId(user.getUserId());
        authResponseDTO.setName(user.getName());
        authResponseDTO.setEmail(user.getEmail());
        return authResponseDTO;
    }
}

