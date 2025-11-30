package com.citi.tts.apibrick.model.swagger.response;

import lombok.Data;

/**
 * Base Response Model
 * Parent class for all API responses
 */
@Data
public class BaseResponse {
    
    /**
     * Response code (200 for success, others for errors)
     */
    private Integer code;
    
    /**
     * Response message
     */
    private String msg;
    
    /**
     * Response data (can be null)
     */
    private Object data;
}

