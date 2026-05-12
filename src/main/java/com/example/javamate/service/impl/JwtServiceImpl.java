package com.example.javamate.service.impl;

import com.example.javamate.entity.User;
import com.example.javamate.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.example.javamate.constants.AppConstants.CLAIM_EMAIL;
import static com.example.javamate.constants.AppConstants.CLAIM_USER_ID;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-days}")
    private int expirationDays;

    private SecretKey key;


    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + getExpirationTimeInMillis());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Override
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object rawUserId = claims.get(CLAIM_USER_ID);

        if (rawUserId instanceof Long value) {
            return value;
        }

        if (rawUserId instanceof Integer value) {
            return value.longValue();
        }

        if (rawUserId instanceof Number value) {
            return value.longValue();
        }

        if (rawUserId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse userId claim '{}' to Long", value);
            }
        }

        return null;
    }

    @Override
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long getExpirationTimeInSeconds() {
        return (long) expirationDays * 24 * 60 * 60;
    }

    private long getExpirationTimeInMillis() {
        return getExpirationTimeInSeconds() * 1000;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}

