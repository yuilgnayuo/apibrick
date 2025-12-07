package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.core.workflow.engine.WorkflowResult;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Response Formatter
 * <p>
 * Formats workflow execution results into HTTP responses based on:
 * - ApiDefinition responseDef (Swagger response schema)
 * - WorkflowResult (execution output)
 * - HTTP status codes
 */
@Service
public class ResponseFormatter {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFormatter.class);

    /**
     * Format successful response
     *
     * @param workflowResult Workflow execution result
     * @param apiDef         ApiDefinition containing response definition
     * @return ResponseEntity with formatted response
     */
    public ResponseEntity<Object> formatResponse(WorkflowResult workflowResult, ApiDefinition apiDef) {
        if (!workflowResult.isSuccess()) {
            return formatErrorResponse(workflowResult, apiDef);
        }

        // Determine HTTP status code from responseDef or default to 200
        String httpStatusCode = determineHttpStatusCode(apiDef);

        // Format response body
        Map<String, Object> responseBody = formatResponseBody(workflowResult, apiDef, httpStatusCode);

        logger.debug("Formatted response: status={}, body keys={}",
                httpStatusCode, responseBody.keySet());

        return ResponseEntity.status(workflowResult.getStatusCode())
                .body(responseBody);
    }

    /**
     * Format error response
     *
     * @param workflowResult Workflow execution result (with error)
     * @param apiDef         ApiDefinition
     * @return ResponseEntity with error response
     */
    public ResponseEntity<Object> formatErrorResponse(WorkflowResult workflowResult, ApiDefinition apiDef) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 500);
        errorBody.put("msg", workflowResult.getErrorMsg() != null ?
                workflowResult.getErrorMsg() : "Internal server error");
        errorBody.put("data", null);

        // Check if responseDef has error response schema
        Map<String, Map<String, Object>> responseDef = apiDef.getResponseDef();
        if (responseDef != null && responseDef.containsKey("500")) {
            // Apply error schema if available
            // Future: apply error schema structure from responseDef
            logger.error("500 error response schema found in ApiDefinition, {}",
                    responseDef.get("500"));
        }

        logger.warn("Formatted error response: {}", errorBody.get("msg"));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody);
    }

    /**
     * Format error response from exception
     *
     * @param error  Exception
     * @param apiDef ApiDefinition
     * @return ResponseEntity with error response
     */
    public ResponseEntity<Object> formatErrorResponse(Throwable error, ApiDefinition apiDef) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 500);
        errorBody.put("msg", error.getMessage() != null ? error.getMessage() : "Internal server error");
        errorBody.put("detail", error.getClass().getSimpleName());
        errorBody.put("data", null);

        logger.error("Formatted exception response: {}", error.getMessage(), error);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody);
    }

    /**
     * Format error response for API not found
     *
     * @return ResponseEntity with 404 error
     */
    public ResponseEntity<Object> formatNotFoundResponse() {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 404);
        errorBody.put("msg", "API not found");
        errorBody.put("data", null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody);
    }

    /**
     * Format error response for parameter validation failure
     *
     * @param errorMessage Error message
     * @return ResponseEntity with 400 error
     */
    public ResponseEntity<Object> formatValidationErrorResponse(String errorMessage) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 400);
        errorBody.put("msg", "Parameter validation failed");
        errorBody.put("detail", errorMessage);
        errorBody.put("data", null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody);
    }

    /**
     * Format response body based on ApiDefinition responseDef
     *
     * @param workflowResult Workflow execution result
     * @param apiDef         ApiDefinition
     * @param httpStatusCode HTTP status code
     * @return Formatted response body map
     */
    private Map<String, Object> formatResponseBody(WorkflowResult workflowResult,
                                                   ApiDefinition apiDef,
                                                   String httpStatusCode) {
        Map<String, Object> responseBody = new HashMap<>();

        // Get response definition for this status code
        Map<String, Map<String, Object>> responseDef = apiDef.getResponseDef();

        if (responseDef != null && responseDef.containsKey(httpStatusCode)) {
            // Apply schema structure if available
            // For now, use standard format
            // Future: apply schema structure from responseDef
            logger.info("Applying response schema for status {}: {}",
                    httpStatusCode, responseDef.get(httpStatusCode));
        }

        // Standard response format: {code, message, data}
        responseBody.put("code", workflowResult.getOutput().get("code"));
        responseBody.put("message", workflowResult.getOutput().get("message"));

        // Extract data from workflow result
        Map<String, Object> output = workflowResult.getOutput();
        if (output != null) {
            // Try to extract final response from context
            Object finalResponse = output.get("finalResponse");
            if (finalResponse != null) {
                responseBody.put("data", finalResponse);
            } else {
                // Use output directly
                responseBody.put("data", output);
            }
        } else {
            responseBody.put("data", null);
        }

        return responseBody;
    }

    /**
     * Determine HTTP status code from ApiDefinition responseDef
     *
     * @param apiDef ApiDefinition
     * @return HTTP status code
     */
    private String determineHttpStatusCode(ApiDefinition apiDef) {
        Map<String, Map<String, Object>> responseDef = apiDef.getResponseDef();
        if (responseDef != null && !responseDef.isEmpty()) {
            // Return first available status code, or default
            return responseDef.keySet().stream()
                    .filter("200"::equals) // Success codes
                    .findFirst()
                    .orElse("200");
        }
        return "200";
    }
}

