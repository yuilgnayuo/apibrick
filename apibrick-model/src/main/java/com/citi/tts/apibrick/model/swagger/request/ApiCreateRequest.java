package com.citi.tts.apibrick.model.swagger.request;

import lombok.Data;

import java.util.List;

/**
 * Create API Request Body
 */
@Data
public class ApiCreateRequest {
    
    /**
     * API name
     */
    private String apiName;
    
    /**
     * API request path (must start with /)
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
     * Encryption algorithm (AES/RSA)
     */
    private String encryptAlgorithm;
    
    /**
     * Associated data source ID list
     */
    private List<String> dataSourceIds;
    
    /**
     * Groovy script content
     */
    private String groovyScript;
    
    /**
     * Request configuration
     */
    private RequestConfig requestConfig;
    
    /**
     * Response configuration
     */
    private ResponseConfig responseConfig;
    
    /**
     * API status (ENABLE, DISABLE, DRAFT)
     */
    private String status;
}

