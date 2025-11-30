package com.citi.tts.apibrick.core.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Configuration for Data Source Manager
 * Initializes DataSourceManager and discovers data source factories
 */
@Configuration
public class DataSourceConfiguration {
    
    @Bean
    public DataSourceManager dataSourceManager() {
        DataSourceManager manager = new DataSourceManager();
        manager.initialize();
        return manager;
    }
}

