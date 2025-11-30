package com.citi.tts.apibrick.model.api;

import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * API Endpoint Model
 * Represents a parsed API endpoint from Swagger
 */

@Data
@ToString(callSuper = true)
public class APIEndpoint {

    private String apiId;              // Unique API identifier
    private String path;               // API path
    private String method;             // HTTP method (GET, POST, PUT, DELETE)
    private Map<String, String> reqHeaders;// Request headers
    private Map<String, Object> body;   // Request body structure
    private Map<String, String> resHeaders;// Response headers
    private Map<String, Object> response;// Response structure

}