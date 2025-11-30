package com.citi.tts.apibrick.core.workflow.engine;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow execution context
 * Stores all data shared between steps during pipeline execution
 * 
 * Context is lightweight and designed for efficient serialization
 * All variables are stored in a key-value map for flexibility
 */
public class StepContext {
    
    @Getter
    private final String flowId;           // Flow definition ID
    @Getter
    private final String flowInstanceId;   // Unique instance ID for this execution
    @Getter
    private final String tenantId;         // Tenant ID for multi-tenancy isolation
    @Getter
    private final String env;              // Environment (DEV/CTE)
    private final Map<String, Object> requestParams; // Original request parameters
    private final Map<String, Object> variables;     // Intermediate variables shared between steps
    
    public StepContext(String flowId, String flowInstanceId, String tenantId, String env, 
                      Map<String, Object> requestParams) {
        this.flowId = flowId;
        this.flowInstanceId = flowInstanceId;
        this.tenantId = tenantId;
        this.env = env;
        this.requestParams = requestParams != null ? requestParams : new HashMap<>();
        this.variables = new HashMap<>();
    }
    
    /**
     * Get a variable from context by key
     * Used by steps to read intermediate results from previous steps
     * 
     * @param key Variable key
     * @param <T> Expected type
     * @return Variable value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) variables.get(key);
    }
    
    /**
     * Set a variable in context
     * Used by steps to store results for subsequent steps
     * 
     * @param key Variable key
     * @param value Variable value
     */
    public void set(String key, Object value) {
        variables.put(key, value);
    }
    
    /**
     * Get request parameter by key
     * 
     * @param key Parameter key
     * @param <T> Expected type
     * @return Parameter value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getRequestParam(String key) {
        return (T) requestParams.get(key);
    }
    
    /**
     * Get all request parameters
     */
    public Map<String, Object> getRequestParams() {
        return new HashMap<>(requestParams);
    }
    
    /**
     * Get all context variables
     */
    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }
    
}

