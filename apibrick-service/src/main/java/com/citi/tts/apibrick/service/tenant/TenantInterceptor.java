package com.citi.tts.apibrick.service.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Tenant Interceptor - WebFilter to extract and propagate tenant ID
 * 
 * Extracts tenant ID from HTTP headers or request parameters
 * and adds it to the reactive context for downstream processing
 */
@Component
public class TenantInterceptor implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String TENANT_ID_PARAM = "tenantId";
    private static final String ENV_HEADER = "X-Env";
    private static final String DEFAULT_ENV = "DEV";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Extract tenant ID from header or parameter
        String tenantId = extractTenantId(request);
        String env = extractEnv(request);
        
        if (tenantId == null || tenantId.isEmpty()) {
            logger.warn("Tenant ID not found in request. Path: {}", request.getPath());
            // In production, you might want to return an error here
            // For now, we'll use a default tenant ID
            tenantId = "default-tenant";
        }
        
        logger.debug("Request tenantId={}, env={}, path={}", tenantId, env, request.getPath());
        
        // Add tenant ID and environment to reactive context
        String finalTenantId = tenantId;
        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put("tenantId", finalTenantId).put("env", env));
    }
    
    /**
     * Extract tenant ID from request headers or parameters
     */
    private String extractTenantId(ServerHttpRequest request) {
        // Try header first
        HttpHeaders headers = request.getHeaders();
        String tenantId = headers.getFirst(TENANT_ID_HEADER);
        
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }
        
        // Try query parameter
        return request.getQueryParams().getFirst(TENANT_ID_PARAM);
    }
    
    /**
     * Extract environment from request headers
     */
    private String extractEnv(ServerHttpRequest request) {
        String env = request.getHeaders().getFirst(ENV_HEADER);
        return env != null && !env.isEmpty() ? env : DEFAULT_ENV;
    }
}

