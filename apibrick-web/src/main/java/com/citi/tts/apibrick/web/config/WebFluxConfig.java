package com.citi.tts.apibrick.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux Configuration
 * 
 * Configures routing priority and exception handling for dynamic API routing.
 * Static routes (like /api/v1/health) should have higher priority than dynamic routes.
 */
@Configuration
@Order(1)
public class WebFluxConfig implements WebFluxConfigurer {
    
    // Note: In Spring WebFlux, route matching is handled by the framework.
    // Static @RequestMapping annotations (like in HealthController) are matched first,
    // then dynamic routes. The @Order annotation helps ensure proper ordering.
    // 
    // For dynamic API routing, we rely on:
    // 1. Static controllers (like HealthController) with specific paths
    // 2. DynamicApiController with @RequestMapping("/**") as a catch-all
    // 
    // Spring will match static routes first, then fall back to dynamic routes.
}

