package com.example.javamate.security;

import com.example.javamate.entity.User;
import com.example.javamate.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserContext {


    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .switchIfEmpty(Mono.error(AppException.unauthorized("User not authenticated")));
    }


    public Mono<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getUserId);
    }


    public Mono<String> getCurrentUserEmail() {
        return getCurrentUser().map(User::getEmail);
    }
}

