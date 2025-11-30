package com.citi.tts.apibrick.model.swagger.request;

import lombok.Data;

/**
 * Response Configuration
 */
@Data
public class ResponseConfig {
    
    /**
     * Content type
     */
    private String contentType;
    
    /**
     * Success response code
     */
    private Integer successCode;
    
    /**
     * Failure response code
     */
    private Integer failCode;
}

