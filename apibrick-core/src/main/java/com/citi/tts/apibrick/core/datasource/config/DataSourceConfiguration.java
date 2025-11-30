package com.citi.tts.apibrick.core.datasource.config;

import com.citi.tts.apibrick.core.datasource.manager.DataSourceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

