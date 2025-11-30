package com.citi.tts.apibrick.model.swagger;

import com.citi.tts.apibrick.model.api.APIEndpoint;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swagger Parser Service
 * Parses Swagger/OpenAPI files into ApiEndpoint models
 */
@Slf4j
public class SwaggerParserService {

    /**
     * Parse Swagger JSON content into API endpoints
     * @param json Swagger JSON string
     * @return List of parsed API endpoints
     */
    public List<APIEndpoint> parseSwaggerJson(String json) {
        List<APIEndpoint> endpoints = new ArrayList<>();
        
        // Parse Swagger using the official Swagger parser library
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        io.swagger.v3.parser.core.models.SwaggerParseResult parseResult = 
            parser.readContents(json, null, new ParseOptions());
        OpenAPI openAPI = parseResult.getOpenAPI();
        
        if (openAPI == null || openAPI.getPaths() == null) {
            return endpoints;
        }
        
        // Process each path and its operations
        openAPI.getPaths().forEach((path, pathItem) -> {
            // Process each HTTP method for the path
            processOperation(path, pathItem.getGet(), "GET", endpoints);
            processOperation(path, pathItem.getPost(), "POST", endpoints);
            processOperation(path, pathItem.getPut(), "PUT", endpoints);
            processOperation(path, pathItem.getDelete(), "DELETE", endpoints);
        });
        
        return endpoints;
    }

    /**
     * Parse Swagger file from filesystem
     * @param filePath Path to Swagger JSON/YAML file
     * @return List of parsed API endpoints
     */
    public List<APIEndpoint> parseSwaggerFile(String filePath) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        io.swagger.v3.parser.core.models.SwaggerParseResult parseResult = 
            parser.readLocation(filePath, null, new ParseOptions());
        OpenAPI openAPI = parseResult.getOpenAPI();
        return extractEndpointsFromOpenAPI(openAPI);
    }

    /**
     * Parse Swagger from URL
     * @param url URL to Swagger JSON/YAML
     * @return List of parsed API endpoints
     */
    public List<APIEndpoint> parseSwaggerUrl(String url) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        io.swagger.v3.parser.core.models.SwaggerParseResult parseResult = 
            parser.readLocation(url, null, new ParseOptions());
        OpenAPI openAPI = parseResult.getOpenAPI();
        return extractEndpointsFromOpenAPI(openAPI);
    }
    
    /**
     * Extract API endpoints from OpenAPI object
     */
    private List<APIEndpoint> extractEndpointsFromOpenAPI(OpenAPI openAPI) {
        List<APIEndpoint> endpoints = new ArrayList<>();
        
        if (openAPI != null && openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                processOperation(path, pathItem.getGet(), "GET", endpoints);
                processOperation(path, pathItem.getPost(), "POST", endpoints);
            });
        }
        
        return endpoints;
    }
    
    /**
     * Process a single API operation (HTTP method) and convert to ApiEndpoint
     */
    private void processOperation(String path, Operation operation, String method, List<APIEndpoint> endpoints) {
        if (operation == null) {
            return;
        }

        // Create new API endpoint
        APIEndpoint endpoint = new APIEndpoint();
        endpoint.setPath(path);
        endpoint.setMethod(method.toUpperCase());

        // Generate unique API ID from path and method
        String apiId = path.replaceAll("[^a-zA-Z0-9]", "-") + "-" + method.toLowerCase();
        endpoint.setApiId(apiId);

        // Extract request headers from parameters
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> pathParams = new HashMap<>();
        Map<String, Object> queryParams = new HashMap<>();
        
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                String paramName = param.getName();
                String paramIn = param.getIn();
                Schema<?> paramSchema = param.getSchema();
                
                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("name", paramName);
                paramInfo.put("required", param.getRequired() != null && param.getRequired());
                paramInfo.put("description", param.getDescription());
                
                if (paramSchema != null) {
                    paramInfo.put("type", paramSchema.getType());
                    paramInfo.put("format", paramSchema.getFormat());
                    if (paramSchema.getExample() != null) {
                        paramInfo.put("example", paramSchema.getExample());
                    }
                }
                
                switch (paramIn != null ? paramIn.toLowerCase() : "") {
                    case "header":
                        headers.put(paramName, param.getDescription());
                        break;
                    case "path":
                        pathParams.put(paramName, paramInfo);
                        break;
                    case "query":
                        queryParams.put(paramName, paramInfo);
                        break;
                }
            }
        }
        endpoint.setReqHeaders(headers);

        // Extract request body schema
        if (operation.getRequestBody() != null) {
            Content requestContent = operation.getRequestBody().getContent();
            if (requestContent != null && requestContent.get("application/json") != null) {
                MediaType mediaType = requestContent.get("application/json");
                Schema<?> bodySchema = mediaType.getSchema();
                if (bodySchema != null) {
                    Map<String, Object> bodyStructure = extractSchemaStructure(bodySchema);
                    endpoint.setBody(bodyStructure);
                }
            }
        }

        // Extract response schema
        if (operation.getResponses() != null) {
            Map<String, Object> responseStructure = new HashMap<>();
            
            // Process 200 response (success)
            ApiResponse successResponse = operation.getResponses().get("200");
            if (successResponse != null) {
                Content responseContent = successResponse.getContent();
                if (responseContent != null && responseContent.get("application/json") != null) {
                    MediaType mediaType = responseContent.get("application/json");
                    Schema<?> responseSchema = mediaType.getSchema();
                    if (responseSchema != null) {
                        responseStructure.put("200", extractSchemaStructure(responseSchema));
                    }
                }
            }
            
            // Process error responses (400, 500, etc.)
            for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
                String statusCode = entry.getKey();
                if (!"200".equals(statusCode)) {
                    ApiResponse errorResponse = entry.getValue();
                    Content errorContent = errorResponse.getContent();
                    if (errorContent != null && errorContent.get("application/json") != null) {
                        MediaType mediaType = errorContent.get("application/json");
                        Schema<?> errorSchema = mediaType.getSchema();
                        if (errorSchema != null) {
                            responseStructure.put(statusCode, extractSchemaStructure(errorSchema));
                        }
                    }
                }
            }
            
            endpoint.setResponse(responseStructure);
        }

        endpoints.add(endpoint);
    }
    
    /**
     * Extract schema structure recursively
     * Converts OpenAPI Schema to Map structure for easier processing
     */
    private Map<String, Object> extractSchemaStructure(Schema<?> schema) {
        Map<String, Object> structure = new HashMap<>();
        
        if (schema == null) {
            return structure;
        }
        
        structure.put("type", schema.getType());
        structure.put("format", schema.getFormat());
        structure.put("description", schema.getDescription());
        
        if (schema.getExample() != null) {
            structure.put("example", schema.getExample());
        }
        
        // Handle object type with properties
        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            Map<String, Object> properties = new HashMap<>();
            for (Map.Entry<String, Schema> propEntry : schema.getProperties().entrySet()) {
                properties.put(propEntry.getKey(), extractSchemaStructure(propEntry.getValue()));
            }
            structure.put("properties", properties);
            
            // Add required fields
            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                structure.put("required", schema.getRequired());
            }
        }
        
        // Handle array type
        if ("array".equals(schema.getType()) && schema.getItems() != null) {
            structure.put("items", extractSchemaStructure(schema.getItems()));
        }
        
        // Handle reference (allOf, oneOf, anyOf)
        if (schema.get$ref() != null) {
            structure.put("$ref", schema.get$ref());
        }
        
        return structure;
    }
}