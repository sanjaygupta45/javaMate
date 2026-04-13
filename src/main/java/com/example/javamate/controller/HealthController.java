package com.example.javamate.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final Instant startTime = Instant.now();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> healthCheck() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "JavaMate",
                "timestamp", Instant.now().toString(),
                "uptime", getUptime()
        ));
    }

    private String getUptime() {
        long seconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}

