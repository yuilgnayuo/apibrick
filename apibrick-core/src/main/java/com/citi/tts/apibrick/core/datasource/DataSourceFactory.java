package com.citi.tts.apibrick.core.datasource;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Data Source Factory interface
 * Creates data source instances for specific database types
 * 
 * Implementations are discovered via SPI mechanism
 */
public interface DataSourceFactory {
    
    /**
     * Create a data source instance
     * 
     * @param config Configuration (connection info, credentials)
     * @param tenantId Tenant ID for isolation
     * @param env Environment (DEV/CTE)
     * @return Mono<DataSource> New data source instance
     */
    Mono<DataSource> create(Map<String, Object> config, String tenantId, String env);
    
    /**
     * Get the data source type this factory supports
     * 
     * @return DataSourceType enum value
     */
    DataSourceType getSupportedType();
}

