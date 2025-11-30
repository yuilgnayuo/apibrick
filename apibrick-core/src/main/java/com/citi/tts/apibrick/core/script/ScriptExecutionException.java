package com.citi.tts.apibrick.core.script;

/**
 * Exception thrown when script execution fails
 */
public class ScriptExecutionException extends RuntimeException {
    
    public ScriptExecutionException(String message) {
        super(message);
    }
    
    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

