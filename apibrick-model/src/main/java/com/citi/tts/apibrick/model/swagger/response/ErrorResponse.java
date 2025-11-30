package com.citi.tts.apibrick.model.swagger.response;

import lombok.Data;

/**
 * Error Response Model
 * Used for error responses (400, 401, 403, 404, 500, etc.)
 */
@Data
public class ErrorResponse {
    
    /**
     * Error code (customized, e.g., 40001 = invalid parameters)
     */
    private Integer code;
    
    /**
     * Error prompt message
     */
    private String msg;
    
    /**
     * Error detail (optional, for debugging)
     */
    private String detail;
}

