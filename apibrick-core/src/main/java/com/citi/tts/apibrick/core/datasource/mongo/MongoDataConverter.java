package com.citi.tts.apibrick.core.datasource.mongo;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataConverter;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB Data Converter
 * Converts MongoDB Document to JSON-like Map structure
 * Handles ObjectId, nested documents, arrays, etc.
 */
public class MongoDataConverter implements DataConverter {
    
    @Override
    public Map<String, Object> convert(Object originalData, Map<String, Object> fieldMapping) {
        if (!(originalData instanceof Document)) {
            throw new IllegalArgumentException("Expected MongoDB Document, got: " + 
                (originalData != null ? originalData.getClass().getName() : "null"));
        }
        
        Document document = (Document) originalData;
        Map<String, Object> result = new HashMap<>();
        
        // If field mapping is provided, use it; otherwise, convert all fields
        if (fieldMapping != null && !fieldMapping.isEmpty()) {
            for (Map.Entry<String, Object> mapping : fieldMapping.entrySet()) {
                String targetField = mapping.getKey();
                String sourceField = mapping.getValue() != null ? mapping.getValue().toString() : targetField;
                
                Object value = document.get(sourceField);
                result.put(targetField, convertValue(value));
            }
        } else {
            // Convert all fields
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                result.put(entry.getKey(), convertValue(entry.getValue()));
            }
        }
        
        return result;
    }
    
    /**
     * Convert MongoDB-specific types to JSON-compatible types
     */
    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof ObjectId) {
            // Convert ObjectId to String
            return ((ObjectId) value).toHexString();
        } else if (value instanceof Document) {
            // Recursively convert nested documents
            Map<String, Object> nested = new HashMap<>();
            Document nestedDoc = (Document) value;
            for (Map.Entry<String, Object> entry : nestedDoc.entrySet()) {
                nested.put(entry.getKey(), convertValue(entry.getValue()));
            }
            return nested;
        } else if (value instanceof java.util.List) {
            // Convert arrays
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) value;
            return list.stream()
                .map(this::convertValue)
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Return primitive types as-is
        return value;
    }
    
    @Override
    public DataSourceType supportType() {
        return DataSourceType.MONGO;
    }
}

