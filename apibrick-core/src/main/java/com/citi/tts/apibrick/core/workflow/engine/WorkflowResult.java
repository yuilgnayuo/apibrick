package com.citi.tts.apibrick.core.workflow.engine;

import java.util.Map;

/**
 * Workflow execution result
 * Contains the final output, execution status, and metadata
 */
public class WorkflowResult {
    
    private boolean success;
    private Map<String, Object> output;
    private String errorMsg;
    private long executeTime;      // Total execution time in milliseconds
    private String flowInstanceId; // Unique instance ID
    
    public WorkflowResult() {
    }
    
    public WorkflowResult(boolean success, Map<String, Object> output, String errorMsg, 
                         long executeTime, String flowInstanceId) {
        this.success = success;
        this.output = output;
        this.errorMsg = errorMsg;
        this.executeTime = executeTime;
        this.flowInstanceId = flowInstanceId;
    }
    
    /**
     * Create a successful workflow result
     */
    public static WorkflowResult success(Map<String, Object> output, long executeTime, String flowInstanceId) {
        return new WorkflowResult(true, output, null, executeTime, flowInstanceId);
    }
    
    /**
     * Create a failed workflow result
     */
    public static WorkflowResult failure(String errorMsg, long executeTime, String flowInstanceId) {
        return new WorkflowResult(false, null, errorMsg, executeTime, flowInstanceId);
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
    
    public String getFlowInstanceId() {
        return flowInstanceId;
    }
    
    public void setFlowInstanceId(String flowInstanceId) {
        this.flowInstanceId = flowInstanceId;
    }
}

