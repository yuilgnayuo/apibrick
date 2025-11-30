package com.citi.tts.apibrick.model.swagger.vo;

import com.citi.tts.apibrick.model.swagger.request.RequestConfig;
import com.citi.tts.apibrick.model.swagger.request.ResponseConfig;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Detail VO
 * Contains complete configuration information
 */
@Data
public class ApiDetailVO {
    
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
    
    /**
     * Creation time
     */
    private LocalDateTime createTime;
    
    /**
     * Update time
     */
    private LocalDateTime updateTime;
}

