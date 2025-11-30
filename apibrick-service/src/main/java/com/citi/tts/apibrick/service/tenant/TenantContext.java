package com.citi.tts.apibrick.service.tenant;

import reactor.util.context.Context;

/**
 * Tenant Context - Manages tenant information in reactive WebFlux environment
 * 
 * Uses Reactor Context instead of ThreadLocal for WebFlux compatibility
 * Tenant ID is automatically propagated through the reactive chain
 */
public class TenantContext {
    
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String ENV_KEY = "env";
    
    /**
     * Get tenant ID from Reactor Context
     * 
     * @param context Reactor Context
     * @return Tenant ID or null if not set
     */
    public static String getTenantId(Context context) {
        return context.getOrDefault(TENANT_ID_KEY, null);
    }
    
    /**
     * Get environment from Reactor Context
     * 
     * @param context Reactor Context
     * @return Environment (DEV/CTE) or null if not set
     */
    public static String getEnv(Context context) {
        return context.getOrDefault(ENV_KEY, null);
    }
    
    /**
     * Create a Context with tenant ID
     * 
     * @param tenantId Tenant ID
     * @return Context with tenant ID
     */
    public static Context withTenantId(String tenantId) {
        return Context.of(TENANT_ID_KEY, tenantId);
    }
    
    /**
     * Create a Context with tenant ID and environment
     * 
     * @param tenantId Tenant ID
     * @param env Environment (DEV/CTE)
     * @return Context with tenant ID and environment
     */
    public static Context withTenant(String tenantId, String env) {
        return Context.of(TENANT_ID_KEY, tenantId)
                     .put(ENV_KEY, env);
    }
    
    /**
     * Add tenant ID to existing Context
     * 
     * @param context Existing Context
     * @param tenantId Tenant ID
     * @return New Context with tenant ID added
     */
    public static Context addTenantId(Context context, String tenantId) {
        return context.put(TENANT_ID_KEY, tenantId);
    }
    
    /**
     * Add environment to existing Context
     * 
     * @param context Existing Context
     * @param env Environment
     * @return New Context with environment added
     */
    public static Context addEnv(Context context, String env) {
        return context.put(ENV_KEY, env);
    }
}

