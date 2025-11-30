package com.citi.tts.apibrick.core.workflow.engine;

import java.util.Map;

/**
 * Step Definition - Configuration for a single step in a workflow
 * Contains step type, configuration parameters, and execution settings
 */
public class StepDefinition {
    
    private String id;                    // Unique step ID within the flow
    private String type;                  // Step type (must match registered Step.getType())
    private Map<String, Object> config;   // Step-specific configuration
    private String outputKey;             // Key to store step output in context
    private FailureStrategy failureStrategy; // How to handle step failure
    private int retryCount;               // Number of retries on failure
    private long retryInterval;           // Retry interval in milliseconds
    
    public StepDefinition() {
        this.failureStrategy = FailureStrategy.TERMINATE;
        this.retryCount = 0;
        this.retryInterval = 100;
    }
    
    public StepDefinition(String id, String type, Map<String, Object> config) {
        this();
        this.id = id;
        this.type = type;
        this.config = config;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    
    public String getOutputKey() {
        return outputKey;
    }
    
    public void setOutputKey(String outputKey) {
        this.outputKey = outputKey;
    }
    
    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }
    
    public void setFailureStrategy(FailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public long getRetryInterval() {
        return retryInterval;
    }
    
    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }
    
    /**
     * Failure handling strategy
     */

}

