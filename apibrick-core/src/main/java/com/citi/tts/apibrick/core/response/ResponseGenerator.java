package com.citi.tts.apibrick.core.response;

import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response Generator - Dynamically generates API responses based on request and workflow execution results
 * 
 * Features:
 * - EL expression support for dynamic value extraction
 * - Field mapping (source field -> target field)
 * - Conditional response generation
 * - Data transformation and masking
 * - Support for nested structures
 */
@Component
public class ResponseGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseGenerator.class);
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * Generate response based on response template and execution context
     * 
     * @param responseTemplate Response template configuration
     * @param context Workflow execution context containing all step results
     * @return Generated response as Map
     */
    public Map<String, Object> generate(Map<String, Object> responseTemplate, StepContext context) {
        if (responseTemplate == null || responseTemplate.isEmpty()) {
            return generateDefaultResponse(context);
        }
        
        Map<String, Object> response = new HashMap<>();
        
        // Process each field in response template
        for (Map.Entry<String, Object> entry : responseTemplate.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldConfig = entry.getValue();
            
            Object fieldValue = processField(fieldConfig, context);
            response.put(fieldName, fieldValue);
        }
        
        return response;
    }
    
    /**
     * Process a single response field
     * 
     * @param fieldConfig Field configuration (can be static value, EL expression, or nested structure)
     * @param context Execution context
     * @return Processed field value
     */
    @SuppressWarnings("unchecked")
    private Object processField(Object fieldConfig, StepContext context) {
        if (fieldConfig == null) {
            return null;
        }
        
        // If fieldConfig is a Map, it might be a field mapping configuration
        if (fieldConfig instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) fieldConfig;
            
            // Check if it's a field mapping configuration
            String sourceType = (String) configMap.get("sourceType");
            
            if (sourceType != null) {
                return processFieldMapping(configMap, context);
            }
            
            // Otherwise, it's a nested object - process recursively
            Map<String, Object> nestedObject = new HashMap<>();
            for (Map.Entry<String, Object> nestedEntry : configMap.entrySet()) {
                nestedObject.put(nestedEntry.getKey(), processField(nestedEntry.getValue(), context));
            }
            return nestedObject;
        }
        
        // If fieldConfig is a List, process each element
        if (fieldConfig instanceof List) {
            List<Object> configList = (List<Object>) fieldConfig;
            return configList.stream()
                .map(item -> processField(item, context))
                .toList();
        }
        
        // If fieldConfig is a String, check if it's an EL expression
        if (fieldConfig instanceof String) {
            String stringValue = (String) fieldConfig;
            
            // Check if it's an EL expression (starts with ${ or #{
            if (stringValue.startsWith("${") && stringValue.endsWith("}")) {
                return evaluateExpression(stringValue.substring(2, stringValue.length() - 1), context);
            }
            
            // Static string value
            return stringValue;
        }
        
        // Static value (number, boolean, etc.)
        return fieldConfig;
    }
    
    /**
     * Process field mapping configuration
     * 
     * Field mapping configuration format:
     * {
     *   "sourceType": "fixed|stepOutput|requestParam|expression",
     *   "sourceValue": "value or EL expression",
     *   "defaultValue": "default value if source is null",
     *   "transform": "mask|encrypt|format",
     *   "condition": "EL expression for conditional mapping"
     * }
     */
    @SuppressWarnings("unchecked")
    private Object processFieldMapping(Map<String, Object> fieldConfig, StepContext context) {
        String sourceType = (String) fieldConfig.get("sourceType");
        Object sourceValue = fieldConfig.get("sourceValue");
        Object defaultValue = fieldConfig.get("defaultValue");
        String condition = (String) fieldConfig.get("condition");
        
        // Check condition first
        if (condition != null && !condition.isEmpty()) {
            Boolean conditionResult = evaluateBooleanExpression(condition, context);
            if (conditionResult == null || !conditionResult) {
                // Condition not met, return default or null
                return defaultValue != null ? defaultValue : null;
            }
        }
        
        Object value = null;
        
        switch (sourceType) {
            case "fixed":
                // Fixed value
                value = sourceValue;
                break;
                
            case "stepOutput":
                // Extract from step output: format "stepId.fieldName" or "${stepId.output.fieldName}"
                if (sourceValue instanceof String) {
                    String sourceStr = (String) sourceValue;
                    value = extractFromStepOutput(sourceStr, context);
                }
                break;
                
            case "requestParam":
                // Extract from request parameters
                if (sourceValue instanceof String) {
                    value = context.getRequestParam((String) sourceValue);
                }
                break;
                
            case "expression":
                // Evaluate EL expression
                if (sourceValue instanceof String) {
                    value = evaluateExpression((String) sourceValue, context);
                }
                break;
                
            default:
                logger.warn("Unknown sourceType: {}", sourceType);
                value = defaultValue;
        }
        
        // Apply default value if value is null
        if (value == null && defaultValue != null) {
            value = defaultValue;
        }
        
        // Apply transformation if specified
        String transform = (String) fieldConfig.get("transform");
        if (transform != null && value != null) {
            value = applyTransformation(value, transform, context);
        }
        
        return value;
    }
    
    /**
     * Extract value from step output
     * Supports formats:
     * - "stepId.fieldName" - direct field access
     * - "${stepId.output.fieldName}" - EL expression
     */
    private Object extractFromStepOutput(String source, StepContext context) {
        // Try direct step output access first
        if (source.contains(".") && !source.startsWith("${")) {
            String[] parts = source.split("\\.", 2);
            String stepId = parts[0];
            String fieldName = parts[1];
            
            String outputKey = stepId + ".output";
            Object stepOutput = context.get(outputKey);
            
            if (stepOutput instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outputMap = (Map<String, Object>) stepOutput;
                return outputMap.get(fieldName);
            }
        }
        
        // Try EL expression
        if (source.startsWith("${") && source.endsWith("}")) {
            return evaluateExpression(source.substring(2, source.length() - 1), context);
        }
        
        // Fallback: try direct context access
        return context.get(source);
    }
    
    /**
     * Evaluate EL expression using Spring Expression Language (SpEL)
     */
    private Object evaluateExpression(String expression, StepContext context) {
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            
            // Add request parameters to context
            evalContext.setVariable("request", context.getRequestParams());
            evalContext.setVariable("requestParams", context.getRequestParams());
            
            // Add context variables
            evalContext.setVariable("variables", context.getVariables());
            evalContext.setVariable("context", context.getVariables());
            
            // Add tenant and env info
            evalContext.setVariable("tenantId", context.getTenantId());
            evalContext.setVariable("env", context.getEnv());
            
            // Add step outputs (each step's output is accessible by stepId)
            Map<String, Object> stepOutputs = new HashMap<>();
            for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
                if (entry.getKey().endsWith(".output")) {
                    String stepId = entry.getKey().substring(0, entry.getKey().length() - 7);
                    stepOutputs.put(stepId, entry.getValue());
                }
            }
            evalContext.setVariable("steps", stepOutputs);
            
            // Parse and evaluate expression
            Expression expr = expressionParser.parseExpression(expression);
            return expr.getValue(evalContext);
            
        } catch (Exception e) {
            logger.warn("Failed to evaluate expression: {}", expression, e);
            return null;
        }
    }
    
    /**
     * Evaluate boolean expression for conditional mapping
     */
    private Boolean evaluateBooleanExpression(String expression, StepContext context) {
        Object result = evaluateExpression(expression, context);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        if (result instanceof String) {
            return Boolean.parseBoolean((String) result);
        }
        return result != null;
    }
    
    /**
     * Apply transformation to value
     * Supported transformations: mask, encrypt, format
     */
    private Object applyTransformation(Object value, String transform, StepContext context) {
        if (value == null) {
            return null;
        }
        
        String stringValue = value.toString();
        
        switch (transform.toLowerCase()) {
            case "mask":
                // Data masking (handled by DataMaskingService in real implementation)
                // For now, simple masking
                if (stringValue.length() > 4) {
                    return "****" + stringValue.substring(stringValue.length() - 4);
                }
                return "****";
                
            case "encrypt":
                // Encryption (handled by EncryptionService in real implementation)
                // For now, return as-is
                return value;
                
            case "format":
                // Format transformation (date, number, etc.)
                return value;
                
            default:
                logger.warn("Unknown transformation: {}", transform);
                return value;
        }
    }
    
    /**
     * Generate default response if no template is provided
     */
    private Map<String, Object> generateDefaultResponse(StepContext context) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", context.getVariables());
        return response;
    }
}

