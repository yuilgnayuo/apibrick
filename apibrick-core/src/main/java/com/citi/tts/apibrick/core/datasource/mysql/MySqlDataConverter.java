package com.citi.tts.apibrick.core.datasource.mysql;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQL Data Converter
 * Converts MySQL ResultSet data to JSON-like Map structure
 * 
 * Handles type conversions:
 * - Timestamp/Datetime -> ISO8601 string
 * - Date -> ISO8601 date string
 * - Time -> ISO8601 time string
 * - BigDecimal -> Number or String
 * - Blob/Clob -> String
 */
public class MySqlDataConverter implements DataConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(MySqlDataConverter.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    public Map<String, Object> convert(Object originalData, Map<String, Object> fieldMapping) {
        if (!(originalData instanceof Map)) {
            throw new IllegalArgumentException("Expected Map<String, Object>, got: " + 
                (originalData != null ? originalData.getClass().getName() : "null"));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> rowData = (Map<String, Object>) originalData;
        Map<String, Object> result = new HashMap<>();
        
        // If field mapping is provided, use it; otherwise, convert all fields
        if (fieldMapping != null && !fieldMapping.isEmpty()) {
            for (Map.Entry<String, Object> mapping : fieldMapping.entrySet()) {
                String targetField = mapping.getKey();
                String sourceField = mapping.getValue() != null ? mapping.getValue().toString() : targetField;
                
                Object value = rowData.get(sourceField);
                result.put(targetField, convertValue(value));
            }
        } else {
            // Convert all fields
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                result.put(entry.getKey(), convertValue(entry.getValue()));
            }
        }
        
        return result;
    }
    
    /**
     * Convert MySQL-specific types to JSON-compatible types
     */
    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        // Handle Timestamp/DateTime
        if (value instanceof Timestamp timestamp) {
            LocalDateTime dateTime = timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            return dateTime.format(DATETIME_FORMATTER);
        }

        // Handle LocalDateTime
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DATETIME_FORMATTER);
        }

        // Handle LocalDate
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMATTER);
        }

        // Handle LocalTime
        if (value instanceof LocalTime) {
            return ((LocalTime) value).format(TIME_FORMATTER);
        }

        // Handle java.sql.Date
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FORMATTER);
        }

        // Handle java.sql.Time
        if (value instanceof java.sql.Time sqlTime) {
            return sqlTime.toLocalTime().format(TIME_FORMATTER);
        }

        // Handle BigDecimal - convert to appropriate number type
        if (value instanceof BigDecimal bigDecimal) {
            // Try to convert to Long if no decimal part
            if (bigDecimal.scale() == 0) {
                try {
                    return bigDecimal.longValueExact();
                } catch (ArithmeticException e) {
                    // Too large for long, return as string
                    return bigDecimal.toPlainString();
                }
            } else {
                // Has decimal part, try to convert to Double
                try {
                    double doubleValue = bigDecimal.doubleValue();
                    // Check if precision is preserved
                    if (BigDecimal.valueOf(doubleValue).compareTo(bigDecimal) == 0) {
                        return doubleValue;
                    } else {
                        // Precision lost, return as string
                        return bigDecimal.toPlainString();
                    }
                } catch (Exception e) {
                    return bigDecimal.toPlainString();
                }
            }
        }

        // Handle byte arrays (BLOB) - convert to base64 string
        if (value instanceof byte[]) {
            return java.util.Base64.getEncoder().encodeToString((byte[]) value);
        }

        // Handle java.sql.Blob
        if (value instanceof java.sql.Blob blob) {
            try {
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                logger.warn("Failed to convert Blob to string", e);
                return null;
            }
        }

        // Handle java.sql.Clob
        if (value instanceof java.sql.Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                logger.warn("Failed to convert Clob to string", e);
                return null;
            }
        }

        // Return primitive types and strings as-is
        return value;
    }
    
    @Override
    public DataSourceType supportType() {
        return DataSourceType.MYSQL;
    }
}