package com.example.javamate.security;

import com.example.javamate.entity.User;
import com.example.javamate.service.JwtService;
import com.example.javamate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());

        // No token -> just continue; AuthorizationWebFilter will decide (permit or 401).
        if (!StringUtils.hasText(token)) {
            return chain.filter(exchange);
        }

        // Token present but invalid/expired -> reject explicitly with 401.
        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid/expired JWT for {} {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);

        if (userId == null && !StringUtils.hasText(email)) {
            log.warn("JWT valid but missing identity claims");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Fallback principal built from token claims, used if DB lookup fails or returns empty.
        User tokenPrincipal = User.builder()
                .userId(userId)
                .email(email)
                .build();

        Mono<User> resolvedUserMono = (userId != null
                ? userService.findById(userId)
                    .switchIfEmpty(Mono.defer(() ->
                            StringUtils.hasText(email) ? userService.findByEmail(email) : Mono.empty()))
                : userService.findByEmail(email))
                .onErrorResume(err -> {
                    log.warn("User lookup failed (userId={}, email={}): {}", userId, email, err.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(tokenPrincipal);

        return resolvedUserMono.flatMap(user -> {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            log.debug("Authenticated user {} for {}", user.getEmail(),
                    exchange.getRequest().getPath().value());

            // contextWrite must wrap chain.filter so the downstream AuthorizationWebFilter
            // (and controllers using ReactiveSecurityContextHolder) can read the auth.
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
        });
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
