package com.citi.tts.apibrick.core.datasource;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * DataSource interface - Unified abstraction for all data sources
 * 
 * Supports multiple database types (Oracle, MySQL, PostgreSQL, MongoDB)
 * All operations are reactive and non-blocking
 */
public interface DataSource {
    
    /**
     * Initialize the data source with configuration
     * Called when data source is created or enabled
     * 
     * @param config Data source configuration (connection info, credentials, etc.)
     * @param tenantId Tenant ID for multi-tenancy isolation
     * @param env Environment (DEV/CTE)
     * @return Mono<Void> indicating initialization completion
     */
    Mono<Void> init(Map<String, Object> config, String tenantId, String env);
    
    /**
     * Execute a query against the data source
     * 
     * @param queryConfig Query configuration (SQL, Mongo query, etc.)
     * @return Mono<Map<String, Object>> Query result as JSON-like structure
     */
    Mono<Map<String, Object>> executeQuery(Map<String, Object> queryConfig);
    
    /**
     * Close the data source and release resources
     * Called when data source is disabled or deleted
     */
    void close();
    
    /**
     * Test data source connection
     * Used for validation during configuration
     * 
     * @return Mono<Boolean> true if connection is valid, false otherwise
     */
    Mono<Boolean> testConnection();
    
    /**
     * Get data source type
     * 
     * @return DataSourceType enum value
     */
    DataSourceType getType();
    
}

