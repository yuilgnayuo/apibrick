package com.citi.tts.apibrick.model.swagger;

import com.citi.tts.apibrick.model.swagger.request.ApiCreateRequest;
import com.citi.tts.apibrick.model.swagger.vo.ApiDetailVO;
import com.citi.tts.apibrick.model.swagger.vo.ApiSimpleVO;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Swagger Parser Usage Example
 * 
 * Demonstrates how to use SwaggerYamlParser to parse OpenAPI YAML files
 * and generate corresponding Java objects
 */
@Slf4j
public class SwaggerParserExample {
    
    public static void main(String[] args) {
        SwaggerYamlParser parser = new SwaggerYamlParser();
        
        // Example 1: Parse YAML from classpath
        try {
            OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
            log.info("Parsed OpenAPI: title={}, version={}", 
                    openAPI.getInfo().getTitle(), 
                    openAPI.getInfo().getVersion());
            
            // Extract all API create requests
            List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
            log.info("Extracted {} API create requests", requests.size());
            
            // Print first request details
            if (!requests.isEmpty()) {
                ApiCreateRequest firstRequest = requests.get(0);
                log.info("First API: name={}, path={}, method={}", 
                        firstRequest.getApiName(), 
                        firstRequest.getApiPath(), 
                        firstRequest.getMethod());
            }
            
        } catch (Exception e) {
            log.error("Failed to parse YAML", e);
        }
        
        // Example 2: Parse YAML from file path
        try {
            String filePath = "src/main/resources/swagger/apibrick.yaml";
            OpenAPI openAPI = parser.parseYamlFromFile(filePath);
            log.info("Successfully parsed YAML from file: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to parse YAML from file", e);
        }
        
        // Example 3: Parse YAML content string
        try {
            String yamlContent = """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /test:
                    get:
                      summary: Test endpoint
                      responses:
                        '200':
                          description: Success
                """;
            
            OpenAPI openAPI = parser.parseYamlContent(yamlContent);
            log.info("Successfully parsed YAML content");
        } catch (Exception e) {
            log.error("Failed to parse YAML content", e);
        }
    }
}

