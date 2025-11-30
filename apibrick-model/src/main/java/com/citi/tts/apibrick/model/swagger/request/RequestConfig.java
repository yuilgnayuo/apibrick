package com.citi.tts.apibrick.model.swagger.request;

import lombok.Data;

import java.util.List;

/**
 * Request Configuration
 */
@Data
public class RequestConfig {
    
    /**
     * Content type
     */
    private String contentType;
    
    /**
     * Request parameters
     */
    private List<RequestParam> params;
    
    /**
     * Request Parameter Definition
     */
    @Data
    public static class RequestParam {
        /**
         * Parameter name
         */
        private String name;
        
        /**
         * Parameter type
         */
        private String type;
        
        /**
         * Whether the parameter is required
         */
        private Boolean required;
    }
}

