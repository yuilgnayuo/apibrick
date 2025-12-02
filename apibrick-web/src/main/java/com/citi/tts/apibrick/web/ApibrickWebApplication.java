package com.citi.tts.apibrick.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Entry Point for Web Layer
 * <p>
 * This is the startup class for the low-code API development platform.
 * It uses Spring Boot WebFlux for reactive programming.
 */
@SpringBootApplication(scanBasePackages = {
        "com.citi.tts.apibrick.web",
        "com.citi.tts.apibrick.service",
        "com.citi.tts.apibrick.common",
        "com.citi.tts.apibrick.core"
})
public class ApibrickWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApibrickWebApplication.class, args);
    }
}

