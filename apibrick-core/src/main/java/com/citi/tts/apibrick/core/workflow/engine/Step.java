package com.citi.tts.apibrick.core.workflow.engine;

import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Step interface - Core abstraction for all workflow steps
 * All steps must implement this interface to be executed by the Pipeline engine
 * 
 * Steps are reactive and return Mono<StepResult> to support non-blocking execution
 * in WebFlux architecture
 */
public interface Step {
    
    /**
     * Execute the step with the given context
     * This is the core method that performs the step's logic
     * 
     * @param context The workflow execution context containing input parameters,
     *                intermediate results, tenant information, etc.
     * @return Mono<StepResult> containing the execution result (output data, status, error info)
     */
    Mono<StepResult> execute(StepContext context);
    
    /**
     * Get the unique step type identifier
     * Used by the StepRegistry to match steps with their implementations
     * and by the frontend to identify step components
     * 
     * @return String representing the step type (e.g., "ORACLE_QUERY", "KAFKA_SEND")
     */
    String getType();
    
    /**
     * Initialize the step with static configuration
     * Called once when the step is registered, before any execution
     * 
     * @param stepConfig Static configuration map (e.g., Kafka broker address, data source connection)
     */
    default void init(Map<String, Object> stepConfig) {
        // Default implementation: no-op
    }
    
    /**
     * Destroy the step and release resources
     * Called when the step is unregistered or the application shuts down
     */
    default void destroy() {
        // Default implementation: no-op
    }
    
    /**
     * Validate step configuration before execution
     * Ensures all required parameters are present and valid
     * 
     * @param stepConfig Step configuration to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    default void validateConfig(Map<String, Object> stepConfig) {
        // Default implementation: basic validation
        if (stepConfig == null) {
            throw new IllegalArgumentException("Step configuration cannot be null");
        }
    }
}

