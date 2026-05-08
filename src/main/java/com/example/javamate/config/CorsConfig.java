package com.example.javamate.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // Exposing CorsConfigurationSource so Spring Security's .cors() picks it up,
    // and CorsWebFilter so CORS is also applied outside the security chain (for OPTIONS).
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "https://java-mate-fe.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Explicitly listing Authorization avoids browser dropping the header after preflight
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsWebFilter corsWebFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsWebFilter(corsConfigurationSource);
    }
}