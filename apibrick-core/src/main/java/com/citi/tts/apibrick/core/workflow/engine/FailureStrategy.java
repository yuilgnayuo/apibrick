package com.citi.tts.apibrick.core.workflow.engine;

public enum FailureStrategy {
        TERMINATE,  // Stop workflow execution immediately
        SKIP,       // Skip this step and continue
        RETRY       // Retry the step (up to retryCount times)
    }