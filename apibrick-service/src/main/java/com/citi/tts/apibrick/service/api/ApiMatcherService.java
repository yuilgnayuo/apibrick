package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.common.enums.ApiStatus;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import com.citi.tts.apibrick.service.repository.ApiDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * API Matcher Service
 * 
 * Matches incoming requests to ApiDefinition based on:
 * - Tenant ID (multi-tenancy isolation)
 * - Request path (exact match, supports path parameters)
 * - HTTP method
 * 
 * Uses caching to improve performance
 */
@Service
public class ApiMatcherService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiMatcherService.class);
    
    @Autowired
    private ApiDefinitionRepository apiDefinitionRepository;
    
    @Autowired
    private ApiCacheManager cacheManager;
    
    /**
     * Match API by tenant ID, request path, and HTTP method
     * 
     * @param tenantId Tenant ID
     * @param requestPath Request path (e.g., /api/v1/user/123)
     * @param httpMethod HTTP method (GET, POST, etc.)
     * @return Mono<ApiDefinition> if matched, Mono.empty() if not found
     */
    public Mono<ApiDefinition> matchApi(String tenantId, String requestPath, String httpMethod) {
        logger.debug("Matching API: tenantId={}, path={}, method={}", tenantId, requestPath, httpMethod);
        
        // Try cache first
        Optional<ApiDefinition> cached = cacheManager.getApiDefinition(tenantId, requestPath, httpMethod);
        if (cached.isPresent()) {
            ApiDefinition apiDef = cached.get();
            // Verify status
            if (apiDef.getApiStatus() == ApiStatus.ENABLED) {
                logger.debug("Matched API from cache: {}", apiDef.getApiCode());
                return Mono.just(apiDef);
            } else {
                logger.debug("API found in cache but status is not ENABLED: {}", apiDef.getApiStatus());
                return Mono.empty();
            }
        }
        
        // Query database - need to match by path pattern
        return Mono.fromCallable(() -> {
            // First, try exact match
            Optional<ApiDefinition> exactMatch = apiDefinitionRepository.findByTenantIdAndApiFix(tenantId, requestPath);
            if (exactMatch.isPresent()) {
                ApiDefinition apiDef = exactMatch.get();
                if (apiDef.getHttpMethod().equalsIgnoreCase(httpMethod) && 
                    apiDef.getApiStatus() == ApiStatus.ENABLED) {
                    // Cache it
                    cacheManager.putApiDefinition(tenantId, requestPath, httpMethod, apiDef);
                    return apiDef;
                }
            }
            
            // If exact match not found, try pattern matching (for path parameters)
            // This requires querying all APIs for the tenant and matching patterns
            // For now, we'll return empty if exact match fails
            // TODO: Implement pattern matching for path parameters like /api/v1/user/{userId}
            return null;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .cast(ApiDefinition.class)
        .doOnSuccess(apiDef -> {
            if (apiDef != null) {
                logger.info("Matched API: {}", apiDef.getApiCode());
            } else {
                logger.debug("No API matched for: tenantId={}, path={}, method={}", tenantId, requestPath, httpMethod);
            }
        })
        .doOnError(error -> {
            logger.error("Error matching API: tenantId={}, path={}, method={}", 
                    tenantId, requestPath, httpMethod, error);
        });
    }
    
    /**
     * Check if request path matches API path pattern
     * Supports path parameters like /api/v1/user/{userId}
     * 
     * @param apiPath API path pattern (e.g., /api/v1/user/{userId})
     * @param requestPath Actual request path (e.g., /api/v1/user/123)
     * @return true if matches, false otherwise
     */
    public boolean isPathMatch(String apiPath, String requestPath) {
        if (apiPath == null || requestPath == null) {
            return false;
        }
        
        // Exact match
        if (apiPath.equals(requestPath)) {
            return true;
        }
        
        // Pattern match: convert {param} to regex
        String pattern = apiPath.replaceAll("\\{[^}]+\\}", "[^/]+");
        Pattern compiledPattern = Pattern.compile("^" + pattern + "$");
        return compiledPattern.matcher(requestPath).matches();
    }
    
    /**
     * Extract path parameters from request path based on API path pattern
     * 
     * @param apiPath API path pattern (e.g., /api/v1/user/{userId})
     * @param requestPath Actual request path (e.g., /api/v1/user/123)
     * @return Map of parameter names to values, empty if no match
     */
    public java.util.Map<String, String> extractPathParams(String apiPath, String requestPath) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        
        if (!isPathMatch(apiPath, requestPath)) {
            return params;
        }
        
        // Extract parameter names from pattern
        java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher paramMatcher = paramPattern.matcher(apiPath);
        java.util.List<String> paramNames = new java.util.ArrayList<>();
        while (paramMatcher.find()) {
            paramNames.add(paramMatcher.group(1));
        }
        
        // Extract values from request path
        String[] apiParts = apiPath.split("/");
        String[] requestParts = requestPath.split("/");
        
        if (apiParts.length != requestParts.length) {
            return params;
        }
        
        for (int i = 0; i < apiParts.length; i++) {
            String apiPart = apiParts[i];
            if (apiPart.startsWith("{") && apiPart.endsWith("}")) {
                String paramName = apiPart.substring(1, apiPart.length() - 1);
                params.put(paramName, requestParts[i]);
            }
        }
        
        return params;
    }
}

