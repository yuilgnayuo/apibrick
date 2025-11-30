package com.citi.tts.apibrick.core.workflow.config;

import com.citi.tts.apibrick.core.workflow.engine.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ServiceLoader;

/**
 * Step Registry - Manages all available step implementations
 * Uses Java SPI (Service Provider Interface) mechanism for dynamic discovery
 * 
 * Steps are automatically discovered and registered at startup
 * New steps can be added by implementing Step interface and registering via SPI
 */
public class StepRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(StepRegistry.class);
    
    private final Map<String, Step> stepMap = new ConcurrentHashMap<>();
    
    /**
     * Initialize the registry by loading all Step implementations via SPI
     */
    public void initialize() {
        logger.info("Initializing Step Registry...");
        
        ServiceLoader<Step> loader = ServiceLoader.load(Step.class);
        int count = 0;
        
        for (Step step : loader) {
            String type = step.getType();
            if (type == null || type.isEmpty()) {
                logger.warn("Step implementation {} has null or empty type, skipping", 
                           step.getClass().getName());
                continue;
            }
            
            if (stepMap.containsKey(type)) {
                logger.warn("Duplicate step type '{}' found. Class {} will be ignored. " +
                          "Existing implementation: {}", 
                          type, step.getClass().getName(), 
                          stepMap.get(type).getClass().getName());
                continue;
            }
            
            stepMap.put(type, step);
            count++;
            logger.debug("Registered step type: {} -> {}", type, step.getClass().getName());
        }
        
        logger.info("Step Registry initialized with {} step types", count);
    }
    
    /**
     * Get step instance by type
     * 
     * @param type Step type identifier
     * @return Step instance or null if not found
     */
    public Step getStep(String type) {
        return stepMap.get(type);
    }
    
    /**
     * Check if a step type is registered
     * 
     * @param type Step type identifier
     * @return true if registered, false otherwise
     */
    public boolean hasStep(String type) {
        return stepMap.containsKey(type);
    }
    
    /**
     * Register a step manually (for testing or dynamic registration)
     * 
     * @param step Step instance to register
     */
    public void registerStep(Step step) {
        String type = step.getType();
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Step type cannot be null or empty");
        }
        stepMap.put(type, step);
        logger.info("Manually registered step type: {} -> {}", type, step.getClass().getName());
    }
    
    /**
     * Get all registered step types
     * 
     * @return Set of all registered step type identifiers
     */
    public java.util.Set<String> getAllStepTypes() {
        return stepMap.keySet();
    }
    
    /**
     * Get the number of registered steps
     */
    public int size() {
        return stepMap.size();
    }
}

