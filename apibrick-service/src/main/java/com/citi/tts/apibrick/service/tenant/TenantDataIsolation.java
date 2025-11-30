package com.citi.tts.apibrick.service.tenant;

import org.springframework.stereotype.Component;
import reactor.util.context.Context;
import java.util.Map;

/**
 * Tenant Data Isolation - Ensures data isolation at the data access layer
 * 
 * Automatically injects tenant_id into queries and filters
 * Ensures Redis keys are prefixed with tenant ID
 */
@Component
public class TenantDataIsolation {
    
    /**
     * Add tenant ID to query parameters
     * Ensures all database queries include tenant_id filter
     * 
     * @param queryParams Original query parameters
     * @param tenantId Tenant ID from context
     * @return Query parameters with tenant_id added
     */
    public Map<String, Object> addTenantFilter(Map<String, Object> queryParams, String tenantId) {
        if (queryParams == null) {
            queryParams = new java.util.HashMap<>();
        }
        queryParams.put("tenant_id", tenantId);
        return queryParams;
    }
    
    /**
     * Prefix Redis key with tenant ID
     * Format: {tenantId}:{resourceType}:{resourceId}
     * 
     * @param key Original key
     * @param tenantId Tenant ID
     * @return Prefixed key
     */
    public String prefixRedisKey(String key, String tenantId) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis key cannot be null or empty");
        }
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        return tenantId + ":" + key;
    }
    
    /**
     * Extract tenant ID from prefixed Redis key
     * 
     * @param prefixedKey Prefixed key (format: {tenantId}:{resourceType}:{resourceId})
     * @return Tenant ID or null if key is not properly formatted
     */
    public String extractTenantFromKey(String prefixedKey) {
        if (prefixedKey == null || !prefixedKey.contains(":")) {
            return null;
        }
        int firstColon = prefixedKey.indexOf(':');
        return prefixedKey.substring(0, firstColon);
    }
    
    /**
     * Get tenant ID from Reactor Context
     * Helper method to extract tenant ID in reactive chains
     * 
     * @param context Reactor Context
     * @return Tenant ID or null if not found
     */
    public String getTenantIdFromContext(Context context) {
        try {
            return context.get("tenantId");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get environment from Reactor Context
     * 
     * @param context Reactor Context
     * @return Environment or "DEV" as default
     */
    public String getEnvFromContext(Context context) {
        try {
            return context.get("env");
        } catch (Exception e) {
            return "DEV";
        }
    }
    
    /**
     * Validate tenant ID is not null or empty
     * Throws exception if tenant ID is invalid
     * 
     * @param tenantId Tenant ID to validate
     * @throws IllegalArgumentException if tenant ID is invalid
     */
    public void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
    }
}

