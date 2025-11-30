package com.citi.tts.apibrick.model.swagger.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API Simple Info VO
 * Used for list display in pagination queries
 */
@Data
public class ApiSimpleVO {
    
    /**
     * Unique API identifier
     */
    private String apiId;
    
    /**
     * API name
     */
    private String apiName;
    
    /**
     * API request path
     */
    private String apiPath;
    
    /**
     * HTTP request method
     */
    private String method;
    
    /**
     * Tenant ID
     */
    private String tenantId;
    
    /**
     * Whether to enable encryption/decryption
     */
    private Boolean encrypt;
    
    /**
     * Groovy script content
     */
    private String groovyScript;
    
    /**
     * API status (ENABLE, DISABLE, DRAFT)
     */
    private String status;
    
    /**
     * Creation time
     */
    private LocalDateTime createTime;
}

