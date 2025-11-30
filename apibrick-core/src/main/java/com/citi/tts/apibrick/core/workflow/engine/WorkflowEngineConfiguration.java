package com.citi.tts.apibrick.core.workflow.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Configuration for Workflow Engine
 * Initializes StepRegistry and PipelineEngine
 */
@Configuration
public class WorkflowEngineConfiguration {
    
    @Bean
    public StepRegistry stepRegistry() {
        StepRegistry registry = new StepRegistry();
        registry.initialize();
        return registry;
    }
}

