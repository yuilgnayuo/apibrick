package com.citi.tts.apibrick.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Health Check Controller
 * 
 * Provides health check endpoint for monitoring and load balancer health checks
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    /**
     * Health check endpoint
     * 
     * GET /api/v1/health
     * 
     * @return Health status information
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "apibrick-web"
        );
        return Mono.just(ResponseEntity.ok(health));
    }
}

