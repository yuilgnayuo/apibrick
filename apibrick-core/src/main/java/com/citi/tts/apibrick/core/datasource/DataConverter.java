package com.citi.tts.apibrick.core.datasource;

import java.util.Map;

/**
 * Data Converter interface - Converts database-specific types to JSON format
 * 
 * Handles type conversions:
 * - Oracle DATE -> ISO8601 string
 * - MongoDB ObjectId -> String
 * - PostgreSQL ARRAY -> JSON array
 * - etc.
 */
public interface DataConverter {
    
    /**
     * Convert database result to JSON-like structure
     * 
     * @param originalData Original database result (ResultSet, Document, etc.)
     * @param fieldMapping Field mapping configuration (source field -> target field)
     * @return Converted data as Map<String, Object>
     */
    Map<String, Object> convert(Object originalData, Map<String, Object> fieldMapping);
    
    /**
     * Get the data source type this converter supports
     * 
     * @return DataSourceType enum value
     */
    DataSourceType supportType();
}

