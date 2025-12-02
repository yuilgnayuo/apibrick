package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.service.domain.ApiDefinition;
import com.citi.tts.apibrick.service.domain.ApiExecuteFlow;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * API Cache Manager
 * 
 * Caches ApiDefinition and ApiExecuteFlow to reduce database queries
 * Uses Caffeine for local caching with TTL and size limits
 */
@Component
public class ApiCacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCacheManager.class);
    
    // Cache key format: tenantId:apiPath:httpMethod
    private static final String CACHE_KEY_FORMAT = "%s:%s:%s";
    
    // Cache for ApiDefinition
    private final Cache<String, ApiDefinition> apiDefinitionCache;
    
    // Cache for ApiExecuteFlow (key: tenantId:flowCode)
    private final Cache<String, ApiExecuteFlow> flowCache;
    
    public ApiCacheManager() {
        // Configure ApiDefinition cache: 1000 entries, 30 minutes TTL
        this.apiDefinitionCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
        
        // Configure ApiExecuteFlow cache: 500 entries, 60 minutes TTL
        this.flowCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * Get ApiDefinition from cache
     * 
     * @param tenantId Tenant ID
     * @param apiPath API path
     * @param httpMethod HTTP method
     * @return Optional ApiDefinition
     */
    public Optional<ApiDefinition> getApiDefinition(String tenantId, String apiPath, String httpMethod) {
        String key = buildApiKey(tenantId, apiPath, httpMethod);
        ApiDefinition apiDef = apiDefinitionCache.getIfPresent(key);
        if (apiDef != null) {
            logger.debug("Cache hit for API: {}", key);
            return Optional.of(apiDef);
        }
        logger.debug("Cache miss for API: {}", key);
        return Optional.empty();
    }
    
    /**
     * Put ApiDefinition into cache
     * 
     * @param tenantId Tenant ID
     * @param apiPath API path
     * @param httpMethod HTTP method
     * @param apiDef ApiDefinition to cache
     */
    public void putApiDefinition(String tenantId, String apiPath, String httpMethod, ApiDefinition apiDef) {
        String key = buildApiKey(tenantId, apiPath, httpMethod);
        apiDefinitionCache.put(key, apiDef);
        logger.debug("Cached API: {}", key);
    }
    
    /**
     * Invalidate ApiDefinition cache
     * 
     * @param tenantId Tenant ID
     * @param apiPath API path
     * @param httpMethod HTTP method
     */
    public void invalidateApi(String tenantId, String apiPath, String httpMethod) {
        String key = buildApiKey(tenantId, apiPath, httpMethod);
        apiDefinitionCache.invalidate(key);
        logger.debug("Invalidated API cache: {}", key);
    }
    
    /**
     * Invalidate all API caches for a tenant
     * 
     * @param tenantId Tenant ID
     */
    public void invalidateTenantApis(String tenantId) {
        // Caffeine doesn't support pattern-based invalidation
        // We need to track keys or clear all (for simplicity, we'll clear all)
        apiDefinitionCache.invalidateAll();
        logger.info("Invalidated all API caches for tenant: {}", tenantId);
    }
    
    /**
     * Get ApiExecuteFlow from cache
     * 
     * @param tenantId Tenant ID
     * @param flowCode Flow code
     * @return Optional ApiExecuteFlow
     */
    public Optional<ApiExecuteFlow> getFlow(String tenantId, String flowCode) {
        String key = buildFlowKey(tenantId, flowCode);
        ApiExecuteFlow flow = flowCache.getIfPresent(key);
        if (flow != null) {
            logger.debug("Cache hit for flow: {}", key);
            return Optional.of(flow);
        }
        logger.debug("Cache miss for flow: {}", key);
        return Optional.empty();
    }
    
    /**
     * Put ApiExecuteFlow into cache
     * 
     * @param tenantId Tenant ID
     * @param flowCode Flow code
     * @param flow ApiExecuteFlow to cache
     */
    public void putFlow(String tenantId, String flowCode, ApiExecuteFlow flow) {
        String key = buildFlowKey(tenantId, flowCode);
        flowCache.put(key, flow);
        logger.debug("Cached flow: {}", key);
    }
    
    /**
     * Invalidate ApiExecuteFlow cache
     * 
     * @param tenantId Tenant ID
     * @param flowCode Flow code
     */
    public void invalidateFlow(String tenantId, String flowCode) {
        String key = buildFlowKey(tenantId, flowCode);
        flowCache.invalidate(key);
        logger.debug("Invalidated flow cache: {}", key);
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("ApiDefinition cache: %s, Flow cache: %s",
                apiDefinitionCache.stats().toString(),
                flowCache.stats().toString());
    }
    
    private String buildApiKey(String tenantId, String apiPath, String httpMethod) {
        return String.format(CACHE_KEY_FORMAT, tenantId, apiPath, httpMethod.toUpperCase());
    }
    
    private String buildFlowKey(String tenantId, String flowCode) {
        return tenantId + ":" + flowCode;
    }
}

