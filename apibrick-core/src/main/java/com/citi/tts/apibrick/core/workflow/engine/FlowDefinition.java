package com.citi.tts.apibrick.core.workflow.engine;

import java.util.List;

/**
 * Flow Definition - Represents a complete workflow pipeline
 * Contains the list of steps and their configurations
 */
public class FlowDefinition {
    
    private String id;
    private String name;
    private String description;
    private List<StepDefinition> steps;
    private ExecutionMode executionMode; // SEQUENTIAL, PARALLEL, BRANCH
    
    public FlowDefinition() {
    }
    
    public FlowDefinition(String id, String name, List<StepDefinition> steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
        this.executionMode = ExecutionMode.SEQUENTIAL;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<StepDefinition> getSteps() {
        return steps;
    }
    
    public void setSteps(List<StepDefinition> steps) {
        this.steps = steps;
    }
    
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }
    
    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }
    
    /**
     * Execution mode for workflow steps
     */
    public enum ExecutionMode {
        SEQUENTIAL,  // Steps executed one after another
        PARALLEL,    // Steps executed concurrently
        BRANCH       // Conditional execution based on conditions
    }
}

