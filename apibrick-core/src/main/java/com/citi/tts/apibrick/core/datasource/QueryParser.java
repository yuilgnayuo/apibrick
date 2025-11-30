package com.citi.tts.apibrick.core.datasource;

import com.citi.tts.apibrick.common.enums.DataSourceType;

import java.util.Map;

/**
 * Query Parser interface - Converts visual configuration to database-native queries
 * 
 * Each database type has its own parser implementation:
 * - SQL Parser for relational databases (Oracle, MySQL, PostgreSQL)
 * - Mongo Query Parser for MongoDB
 */
public interface QueryParser {
    
    /**
     * Parse query configuration into database-native format
     * 
     * @param queryConfig Visual configuration (SQL string, Mongo conditions, etc.)
     * @return Parsed query object (PreparedStatement params, BsonDocument, etc.)
     */
    Object parse(Map<String, Object> queryConfig);
    
    /**
     * Get the data source type this parser supports
     * 
     * @return DataSourceType enum value
     */
    DataSourceType supportType();
    
    /**
     * Validate query configuration
     * Checks for SQL injection, dangerous operations, etc.
     * 
     * @param queryConfig Query configuration to validate
     * @throws IllegalArgumentException if configuration is invalid or dangerous
     */
    default void validate(Map<String, Object> queryConfig) {
        // Default implementation: basic validation
        if (queryConfig == null) {
            throw new IllegalArgumentException("Query configuration cannot be null");
        }
    }
}

