package com.citi.tts.apibrick.model.swagger;

import com.citi.tts.apibrick.model.swagger.request.ApiCreateRequest;
import com.citi.tts.apibrick.model.swagger.request.RequestConfig;
import com.citi.tts.apibrick.model.swagger.request.ResponseConfig;
import com.citi.tts.apibrick.model.swagger.response.*;
import com.citi.tts.apibrick.model.swagger.vo.ApiDetailVO;
import com.citi.tts.apibrick.model.swagger.vo.ApiSimpleVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Swagger YAML Parser
 * 
 * Parses OpenAPI 3.0 YAML files and generates corresponding Java objects
 * Supports parsing all schemas defined in the YAML file
 */
@Slf4j
public class SwaggerYamlParser {
    
    private final ObjectMapper yamlMapper;
    private final OpenAPIV3Parser parser;
    
    public SwaggerYamlParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.parser = new OpenAPIV3Parser();
    }
    
    /**
     * Parse Swagger YAML file from classpath
     * 
     * @param resourcePath Resource path (e.g., "swagger/apibrick.yaml")
     * @return Parsed OpenAPI object
     */
    public OpenAPI parseYamlFromClasspath(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            
            String yamlContent = new String(inputStream.readAllBytes());
            return parseYamlContent(yamlContent);
        } catch (IOException e) {
            log.error("Failed to read YAML file from classpath: {}", resourcePath, e);
            throw new RuntimeException("Failed to parse YAML file", e);
        }
    }
    
    /**
     * Parse Swagger YAML file from filesystem
     * 
     * @param filePath Path to YAML file
     * @return Parsed OpenAPI object
     */
    public OpenAPI parseYamlFromFile(String filePath) {
        try {
            String yamlContent = Files.readString(Paths.get(filePath));
            return parseYamlContent(yamlContent);
        } catch (IOException e) {
            log.error("Failed to read YAML file: {}", filePath, e);
            throw new RuntimeException("Failed to parse YAML file", e);
        }
    }
    
    /**
     * Parse YAML content string
     * 
     * @param yamlContent YAML content as string
     * @return Parsed OpenAPI object
     */
    public OpenAPI parseYamlContent(String yamlContent) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        
        SwaggerParseResult parseResult = parser.readContents(yamlContent, null, options);
        
        if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
            log.warn("Swagger parsing warnings: {}", parseResult.getMessages());
        }
        
        OpenAPI openAPI = parseResult.getOpenAPI();
        if (openAPI == null) {
            throw new RuntimeException("Failed to parse YAML content: " + parseResult.getMessages());
        }
        
        return openAPI;
    }
    
    /**
     * Extract all API operations from OpenAPI and convert to ApiCreateRequest objects
     * 
     * @param openAPI Parsed OpenAPI object
     * @return List of ApiCreateRequest objects
     */
    public List<ApiCreateRequest> extractApiCreateRequests(OpenAPI openAPI) {
        List<ApiCreateRequest> requests = new ArrayList<>();
        
        if (openAPI.getPaths() == null) {
            return requests;
        }
        
        openAPI.getPaths().forEach((path, pathItem) -> {
            extractRequestFromOperation(path, pathItem.getGet(), "GET", requests, openAPI);
            extractRequestFromOperation(path, pathItem.getPost(), "POST", requests, openAPI);
            extractRequestFromOperation(path, pathItem.getPut(), "PUT", requests, openAPI);
            extractRequestFromOperation(path, pathItem.getDelete(), "DELETE", requests, openAPI);
            extractRequestFromOperation(path, pathItem.getPatch(), "PATCH", requests, openAPI);
        });
        
        return requests;
    }
    
    /**
     * Extract ApiCreateRequest from a single operation
     */
    private void extractRequestFromOperation(
            String path, 
            Operation operation, 
            String method,
            List<ApiCreateRequest> requests,
            OpenAPI openAPI) {
        
        if (operation == null) {
            return;
        }
        
        ApiCreateRequest request = new ApiCreateRequest();
        
        // Extract basic information
        request.setApiPath(path);
        request.setMethod(method);
        request.setApiName(operation.getSummary() != null ? operation.getSummary() : operation.getOperationId());
        
        // Extract request body if present
        if (operation.getRequestBody() != null) {
            RequestConfig requestConfig = extractRequestConfig(operation.getRequestBody(), openAPI);
            request.setRequestConfig(requestConfig);
        }
        
        // Extract response configuration
        if (operation.getResponses() != null) {
            ResponseConfig responseConfig = extractResponseConfig(operation.getResponses());
            request.setResponseConfig(responseConfig);
        }
        
        // Extract parameters (query, path, header)
        if (operation.getParameters() != null) {
            RequestConfig config = request.getRequestConfig();
            if (config == null) {
                config = new RequestConfig();
                request.setRequestConfig(config);
            }
            
            List<RequestConfig.RequestParam> params = new ArrayList<>();
            for (Parameter param : operation.getParameters()) {
                RequestConfig.RequestParam requestParam = new RequestConfig.RequestParam();
                requestParam.setName(param.getName());
                requestParam.setType(param.getSchema() != null ? param.getSchema().getType() : "string");
                requestParam.setRequired(param.getRequired() != null && param.getRequired());
                params.add(requestParam);
            }
            config.setParams(params);
        }
        
        // Default status to DRAFT
        request.setStatus("DRAFT");
        
        requests.add(request);
    }
    
    /**
     * Extract RequestConfig from RequestBody
     */
    private RequestConfig extractRequestConfig(RequestBody requestBody, OpenAPI openAPI) {
        RequestConfig config = new RequestConfig();
        
        if (requestBody.getContent() != null) {
            Content content = requestBody.getContent();
            
            // Get content type (prefer application/json)
            String contentType = content.keySet().stream()
                    .filter(ct -> ct.contains("json"))
                    .findFirst()
                    .orElse(content.keySet().iterator().next());
            config.setContentType(contentType);
            
            // Extract parameters from schema
            MediaType mediaType = content.get(contentType);
            if (mediaType != null && mediaType.getSchema() != null) {
                Schema<?> schema = mediaType.getSchema();
                List<RequestConfig.RequestParam> params = extractParamsFromSchema(schema, openAPI);
                config.setParams(params);
            }
        }
        
        return config;
    }
    
    /**
     * Extract parameters from schema
     */
    private List<RequestConfig.RequestParam> extractParamsFromSchema(Schema<?> schema, OpenAPI openAPI) {
        List<RequestConfig.RequestParam> params = new ArrayList<>();
        
        if (schema == null) {
            return params;
        }
        
        // Handle $ref references
        if (schema.get$ref() != null) {
            Schema<?> resolvedSchema = resolveSchemaReference(schema.get$ref(), openAPI);
            if (resolvedSchema != null) {
                schema = resolvedSchema;
            }
        }
        
        // Extract properties from object schema
        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            List<String> requiredFields = schema.getRequired() != null ? schema.getRequired() : Collections.emptyList();
            
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                RequestConfig.RequestParam param = new RequestConfig.RequestParam();
                param.setName(entry.getKey());
                
                Schema<?> propSchema = entry.getValue();
                param.setType(propSchema.getType() != null ? propSchema.getType() : "string");
                param.setRequired(requiredFields.contains(entry.getKey()));
                
                params.add(param);
            }
        }
        
        return params;
    }
    
    /**
     * Extract ResponseConfig from responses
     */
    private ResponseConfig extractResponseConfig(Map<String, ApiResponse> responses) {
        ResponseConfig config = new ResponseConfig();
        config.setContentType("application/json");
        
        // Find success code (200, 201, etc.)
        Integer successCode = responses.keySet().stream()
                .filter(code -> code.startsWith("2"))
                .map(Integer::parseInt)
                .min(Integer::compareTo)
                .orElse(200);
        config.setSuccessCode(successCode);
        
        // Find error code (400, 500, etc.)
        Integer failCode = responses.keySet().stream()
                .filter(code -> code.startsWith("4") || code.startsWith("5"))
                .map(Integer::parseInt)
                .min(Integer::compareTo)
                .orElse(500);
        config.setFailCode(failCode);
        
        return config;
    }
    
    /**
     * Resolve schema reference ($ref)
     */
    private Schema<?> resolveSchemaReference(String ref, OpenAPI openAPI) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        
        String schemaName = ref.substring("#/components/schemas/".length());
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            return (Schema<?>) openAPI.getComponents().getSchemas().get(schemaName);
        }
        
        return null;
    }
    
    /**
     * Generate ApiDetailVO from OpenAPI operation
     */
    public ApiDetailVO generateApiDetailVO(String apiId, Operation operation, OpenAPI openAPI) {
        ApiDetailVO detail = new ApiDetailVO();
        detail.setApiId(apiId);
        detail.setApiName(operation.getSummary() != null ? operation.getSummary() : operation.getOperationId());
        detail.setStatus("ENABLE");
        detail.setCreateTime(java.time.LocalDateTime.now());
        detail.setUpdateTime(java.time.LocalDateTime.now());
        
        // Extract request and response configs
        if (operation.getRequestBody() != null) {
            detail.setRequestConfig(extractRequestConfig(operation.getRequestBody(), openAPI));
        }
        
        if (operation.getResponses() != null) {
            detail.setResponseConfig(extractResponseConfig(operation.getResponses()));
        }
        
        return detail;
    }
    
    /**
     * Generate ApiSimpleVO from OpenAPI operation
     */
    public ApiSimpleVO generateApiSimpleVO(String apiId, String path, String method, Operation operation) {
        ApiSimpleVO simple = new ApiSimpleVO();
        simple.setApiId(apiId);
        simple.setApiPath(path);
        simple.setMethod(method);
        simple.setApiName(operation.getSummary() != null ? operation.getSummary() : operation.getOperationId());
        simple.setStatus("ENABLE");
        simple.setCreateTime(java.time.LocalDateTime.now());
        return simple;
    }
}

