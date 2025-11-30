package com.citi.tts.apibrick.core.datasource.manager;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ServiceLoader;

/**
 * Data Source Manager - Manages all data source instances
 * 
 * Features:
 * - Per-tenant data source isolation
 * - Connection pool management
 * - Dynamic data source registration
 * - SPI-based data source discovery
 */
@Component
public class DataSourceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceManager.class);
    
    // Map: tenantId:datasourceId -> DataSource instance
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    
    // Map: DataSourceType -> DataSource factory
    private final Map<DataSourceType, DataSourceFactory> factoryMap = new ConcurrentHashMap<>();
    
    /**
     * Initialize data source manager
     * Discovers all DataSource implementations via SPI
     */
    public void initialize() {
        logger.info("Initializing Data Source Manager...");
        
        // Load DataSourceFactory implementations via SPI
        ServiceLoader<DataSourceFactory> loader = ServiceLoader.load(DataSourceFactory.class);
        int count = 0;
        
        for (DataSourceFactory factory : loader) {
            DataSourceType type = factory.getSupportedType();
            factoryMap.put(type, factory);
            count++;
            logger.debug("Registered data source factory: {} -> {}", type, factory.getClass().getName());
        }
        
        logger.info("Data Source Manager initialized with {} factory types", count);
    }
    
    /**
     * Get or create a data source instance
     * Data sources are isolated per tenant
     * 
     * @param datasourceId Data source configuration ID
     * @param type Data source type
     * @param config Configuration (connection info, credentials)
     * @param tenantId Tenant ID
     * @param env Environment
     * @return Mono<DataSource> Data source instance
     */
    public Mono<DataSource> getOrCreateDataSource(
            String datasourceId,
            DataSourceType type,
            Map<String, Object> config,
            String tenantId,
            String env) {
        
        String key = buildKey(tenantId, datasourceId);
        
        // Check if data source already exists
        DataSource existing = dataSourceMap.get(key);
        if (existing != null) {
            return Mono.just(existing);
        }
        
        // Create new data source
        DataSourceFactory factory = factoryMap.get(type);
        if (factory == null) {
            return Mono.error(new IllegalArgumentException(
                "Data source type " + type + " is not supported"));
        }
        
        return factory.create(config, tenantId, env)
            .doOnNext(ds -> {
                dataSourceMap.put(key, ds);
                logger.info("Created data source. tenantId={}, datasourceId={}, type={}", 
                           tenantId, datasourceId, type);
            });
    }
    
    /**
     * Get existing data source
     * 
     * @param datasourceId Data source configuration ID
     * @param tenantId Tenant ID
     * @return Mono<DataSource> or Mono.empty() if not found
     */
    public Mono<DataSource> getDataSource(String datasourceId, String tenantId) {
        String key = buildKey(tenantId, datasourceId);
        DataSource ds = dataSourceMap.get(key);
        return ds != null ? Mono.just(ds) : Mono.empty();
    }
    
    /**
     * Remove and close a data source
     * 
     * @param datasourceId Data source configuration ID
     * @param tenantId Tenant ID
     */
    public void removeDataSource(String datasourceId, String tenantId) {
        String key = buildKey(tenantId, datasourceId);
        DataSource ds = dataSourceMap.remove(key);
        if (ds != null) {
            ds.close();
            logger.info("Removed data source. tenantId={}, datasourceId={}", tenantId, datasourceId);
        }
    }
    
    /**
     * Test data source connection
     * 
     * @param type Data source type
     * @param config Configuration
     * @param tenantId Tenant ID
     * @param env Environment
     * @return Mono<Boolean> true if connection is valid
     */
    public Mono<Boolean> testConnection(
            DataSourceType type,
            Map<String, Object> config,
            String tenantId,
            String env) {
        
        DataSourceFactory factory = factoryMap.get(type);
        if (factory == null) {
            return Mono.just(false);
        }
        
        return factory.create(config, tenantId, env)
            .flatMap(DataSource::testConnection)
            .doFinally(signalType -> {
                // Clean up test data source
                // (In real implementation, you might want to cache test connections)
            });
    }
    
    /**
     * Build cache key for data source map
     */
    private String buildKey(String tenantId, String datasourceId) {
        return tenantId + ":" + datasourceId;
    }
}

