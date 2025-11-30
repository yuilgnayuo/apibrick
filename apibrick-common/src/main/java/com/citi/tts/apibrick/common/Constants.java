package com.citi.tts.apibrick.common;

/**
 * Common constants used across the platform
 */
public class Constants {
    
    // HTTP Headers
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_ENV = "X-Env";
    
    // Default values
    public static final String DEFAULT_ENV = "DEV";
    public static final String DEFAULT_TENANT = "default-tenant";
    
    // Timeouts
    public static final long SCRIPT_TIMEOUT_MS = 500;
    public static final long DATASOURCE_TIMEOUT_MS = 3000;
    
    // Memory limits
    public static final long SCRIPT_MEMORY_LIMIT_MB = 100;
    
    private Constants() {
        // Utility class
    }
}

