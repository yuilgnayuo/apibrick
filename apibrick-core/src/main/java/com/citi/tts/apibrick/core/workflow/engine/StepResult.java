package com.citi.tts.apibrick.core.workflow.engine;

import java.util.Map;
import java.util.HashMap;

/**
 * Step execution result
 * Contains the output data, execution status, error information, and performance metrics
 */
public class StepResult {
    
    private boolean success;
    private Map<String, Object> output;
    private String errorMsg;
    private long executeTime; // Execution time in milliseconds
    private String stepId;
    private String stepType;
    
    public StepResult() {
        this.output = new HashMap<>();
    }
    
    public StepResult(boolean success, Map<String, Object> output, String errorMsg, long executeTime) {
        this.success = success;
        this.output = output != null ? output : new HashMap<>();
        this.errorMsg = errorMsg;
        this.executeTime = executeTime;
    }
    
    /**
     * Create a successful step result
     */
    public static StepResult success(Map<String, Object> output, long executeTime) {
        return new StepResult(true, output, null, executeTime);
    }
    
    /**
     * Create a failed step result
     */
    public static StepResult failure(String errorMsg, long executeTime) {
        return new StepResult(false, null, errorMsg, executeTime);
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Map<String, Object> getOutput() {
        return output;
    }
    
    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public long getExecuteTime() {
        return executeTime;
    }
    
    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }
    
    public String getStepType() {
        return stepType;
    }
    
    public void setStepType(String stepType) {
        this.stepType = stepType;
    }
}

