package com.citi.tts.apibrick.common.enums;

public enum FailureStrategy {
        TERMINATE,  // Stop workflow execution immediately
        SKIP,       // Skip this step and continue
        RETRY       // Retry the step (up to retryCount times)
    }