package com.example.javamate.security;

import com.example.javamate.exception.AppException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoogleIdTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierService.class);

    @Value("${google.oauth2.client-ids:}")
    private String googleClientIdsProperty;

    @Value("${google.oauth2.accepted-issuers:https://accounts.google.com,accounts.google.com}")
    private String acceptedIssuersProperty;

    private List<String> googleClientIds;
    private Set<String> acceptedIssuers;
    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        this.googleClientIds = Arrays.stream(googleClientIdsProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        this.acceptedIssuers = Arrays.stream(acceptedIssuersProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (googleClientIds.isEmpty()) {
            log.warn("Google OAuth2 is not configured: no client IDs found");
        } else {
            log.info("Google OAuth2 configured with {} client ID(s)", googleClientIds.size());
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(googleClientIds)
                .build();
    }

    public Mono<GoogleIdToken.Payload> verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            return Mono.error(AppException.unauthorized("Google ID token is missing"));
        }

        if (googleClientIds == null || googleClientIds.isEmpty()) {
            return Mono.error(AppException.unauthorized("Google authentication is not configured"));
        }

        return Mono.fromCallable(() -> verifier.verify(idToken.trim()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(this::mapVerificationFailure)
                .flatMap(token -> {
                    if (token == null) {
                        return Mono.error(AppException.unauthorized("Invalid Google ID token"));
                    }

                    GoogleIdToken.Payload payload = token.getPayload();
                    String issuer = payload.getIssuer();
                    String email = payload.getEmail();

                    if (!acceptedIssuers.isEmpty() && !acceptedIssuers.contains(issuer)) {
                        return Mono.error(AppException.unauthorized("Invalid token issuer"));
                    }

                    if (!isEmailVerified(payload.get("email_verified"))) {
                        return Mono.error(AppException.unauthorized("Google account email is not verified"));
                    }

                    if (!StringUtils.hasText(email)) {
                        return Mono.error(AppException.unauthorized("Google account email is missing"));
                    }

                    return Mono.just(payload);
                });
    }

    private RuntimeException mapVerificationFailure(Throwable throwable) {
        if (throwable instanceof AppException appException) {
            return appException;
        }

        if (throwable instanceof GeneralSecurityException || throwable instanceof IOException) {
            log.warn("Google ID token verification failed: {}", throwable.getMessage());
            return AppException.unauthorized("Unable to verify Google token");
        }

        log.warn("Google token validation error: {}", throwable.getMessage());
        return AppException.unauthorized("Invalid Google token");
    }

    private boolean isEmailVerified(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (rawValue instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }

        return false;
    }
}
