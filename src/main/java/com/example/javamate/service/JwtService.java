package com.example.javamate.service;

import com.example.javamate.entity.User;

public interface JwtService {

    /**
     * Generate JWT token for user with userId and email in payload
     */
    String generateToken(User user);

    /**
     * Extract user ID from token
     */
    Long extractUserId(String token);

    /**
     * Extract email from token
     */
    String extractEmail(String token);

    /**
     * Validate token
     */
    boolean isTokenValid(String token);

    /**
     * Get expiration time in seconds
     */
    Long getExpirationTimeInSeconds();
}

