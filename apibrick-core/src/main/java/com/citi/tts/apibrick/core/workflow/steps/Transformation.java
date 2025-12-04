package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {
 * "stepId": "step_001",
 * "stepName": "Path Parameter Validation & Transformation",
 * "stepType": "PARAM_TRANSFORM",
 * "preStepIds": "",
 * "failStop": true,
 * "stepConfig": {
 * "sourceParam": "id",
 * "targetParam": "userId",
 * "paramType": "PATH",
 * "dataType": "INTEGER",
 * "validationRules": {
 * "required": true,
 * "min": 1,
 * "max": 999999,
 * "errorMsg": "Invalid user ID (must be 1-999999)"
 * }
 * }
 * },
 */
@Slf4j
@Component
public class Transformation implements Step {

    private static final String TYPE = "PARAM_TRANSFORM";

    @Override
    public Mono<StepResult> execute(StepContext context) {
        long startTime = System.currentTimeMillis();

        // Read configuration from context (set by PipelineEngine from StepDefinition.config)
        String sourceParam = context.get("sourceParam");
        String targetParam = context.get("targetParam");
        String dataType = context.get("dataType");   // STRING, INTEGER, LONG, DOUBLE, BOOLEAN
        Map<String, Object> validationRules = context.get("validationRules");

        if (dataType == null || dataType.isEmpty()) {
            dataType = "STRING";
        }
        if (targetParam == null || targetParam.isEmpty()) {
            targetParam = sourceParam;
        }

        // Basic config sanity check (defensive; main validation is in validateConfig)
        if (sourceParam == null || sourceParam.isEmpty()) {
            String msg = "sourceParam is required for PARAM_TRANSFORM step";
            log.error(msg);
            return Mono.just(StepResult.failure(msg, System.currentTimeMillis() - startTime));
        }

        Object rawValue = context.getRequestParam(sourceParam);
        boolean required = getBooleanRule(validationRules, "required", false);
        String customErrorMsg = getStringRule(validationRules, "errorMsg", null);

        // Required check
        if (required && (rawValue == null || (rawValue instanceof String && ((String) rawValue).isBlank()))) {
            String msg = customErrorMsg != null ? customErrorMsg :
                    String.format("Required parameter '%s' is missing", sourceParam);
            log.warn("Parameter validation failed: {}", msg);
            return Mono.just(StepResult.failure(msg, System.currentTimeMillis() - startTime));
        }

        // If not required and value is null/blank – simply skip and return empty success
        if (!required && (rawValue == null || (rawValue instanceof String && ((String) rawValue).isBlank()))) {
            Map<String, Object> output = new HashMap<>();
            return Mono.just(StepResult.success(output, System.currentTimeMillis() - startTime));
        }

        try {
            Object converted = convertValue(rawValue, dataType, validationRules, sourceParam);
            Map<String, Object> output = new HashMap<>();
            output.put(targetParam, converted);
            log.debug("Parameter transformed: sourceParam={}, targetParam={}, value={}",
                    sourceParam, targetParam, converted);
            context.set(targetParam, converted);
            return Mono.just(StepResult.success(output, System.currentTimeMillis() - startTime));
        } catch (IllegalArgumentException ex) {
            String msg = customErrorMsg != null ? customErrorMsg : ex.getMessage();
            log.warn("Parameter transformation failed for '{}': {}", sourceParam, msg);
            return Mono.just(StepResult.failure(msg, System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            String msg = "Unexpected error during parameter transformation: " + ex.getMessage();
            log.error(msg, ex);
            return Mono.just(StepResult.failure(msg, System.currentTimeMillis() - startTime));
        }
    }

    /**
     * Validate static configuration before execution.
     */
    @Override
    public void validateConfig(Map<String, Object> stepConfig) {
        if (stepConfig == null) {
            throw new IllegalArgumentException("stepConfig is required for PARAM_TRANSFORM step");
        }
        Object sourceParam = stepConfig.get("sourceParam");
        if (!(sourceParam instanceof String) || ((String) sourceParam).isBlank()) {
            throw new IllegalArgumentException("stepConfig.sourceParam must be a non-empty string");
        }
        // targetParam is optional; default to sourceParam if missing
    }

    private Object convertValue(Object rawValue,
                                String dataType,
                                Map<String, Object> validationRules,
                                String paramName) {
        String str = String.valueOf(rawValue).trim();
        try {
            return switch (dataType.toUpperCase()) {
                case "INTEGER" -> {
                    long v = Integer.parseInt(str);
                    validateRange(v, validationRules, paramName);
                    yield (int) v;
                }
                case "LONG" -> {
                    long v = Long.parseLong(str);
                    validateRange(v, validationRules, paramName);
                    yield v;
                }
                case "DOUBLE" -> {
                    double v = Double.parseDouble(str);
                    validateRange(v, validationRules, paramName);
                    yield v;
                }
                case "BOOLEAN" -> Boolean.parseBoolean(str);
                case "STRING" -> {
                    int minLen = getNumberRule(validationRules, "minLength", 0);
                    int maxLen = getNumberRule(validationRules, "maxLength", 0);
                    if (str.length() < minLen) {
                        throw new IllegalArgumentException(
                                String.format("Parameter '%s' length must be >= %d", paramName, minLen));
                    }
                    if (str.length() > maxLen) {
                        throw new IllegalArgumentException(
                                String.format("Parameter '%s' length must be <= %d", paramName, maxLen));
                    }
                    yield str;
                }
                default -> {
                    throw new IllegalArgumentException(
                            "Unsupported dataType '" + dataType + "' for PARAM_TRANSFORM step");
                }
            };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' value '%s' is not a valid %s",
                            paramName, str, dataType.toUpperCase()), ex);
        }
    }

    private void validateRange(double value, Map<String, Object> rules, String paramName) {
        if (rules == null) {
            return;
        }
        Integer min = getNumberRule(rules, "min", null);
        Integer max = getNumberRule(rules, "max", null);
        if (min != null && value < min) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' must be >= %s", paramName, min));
        }
        if (max != null && value > max) {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' must be <= %s", paramName, max));
        }
    }

    private boolean getBooleanRule(Map<String, Object> rules, String key, boolean defaultVal) {
        if (rules == null) {
            return defaultVal;
        }
        Object v = rules.get(key);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return defaultVal;
    }

    private String getStringRule(Map<String, Object> rules, String key, String defaultVal) {
        if (rules == null) {
            return defaultVal;
        }
        Object v = rules.get(key);
        return v != null ? Objects.toString(v) : defaultVal;
    }

    private Integer getNumberRule(Map<String, Object> rules, String key, Integer defaultVal) {
        if (rules == null) {
            return defaultVal;
        }
        Object v = rules.get(key);
        if (v == null) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
