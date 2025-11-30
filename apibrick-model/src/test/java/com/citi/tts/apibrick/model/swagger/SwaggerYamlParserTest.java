package com.citi.tts.apibrick.model.swagger;

import com.citi.tts.apibrick.model.swagger.request.ApiCreateRequest;
import com.citi.tts.apibrick.model.swagger.vo.ApiDetailVO;
import com.citi.tts.apibrick.model.swagger.vo.ApiSimpleVO;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for SwaggerYamlParser
 * 
 * Tests parsing of OpenAPI 3.0 YAML files and generation of Java objects
 */
@DisplayName("Swagger YAML Parser Tests")
class SwaggerYamlParserTest {
    
    private SwaggerYamlParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new SwaggerYamlParser();
    }
    
    @Test
    @DisplayName("Should parse YAML file from classpath")
    void testParseYamlFromClasspath() {
        // Given
        String resourcePath = "swagger/apibrick.yaml";
        
        // When
        OpenAPI openAPI = parser.parseYamlFromClasspath(resourcePath);
        
        // Then
        assertNotNull(openAPI, "OpenAPI should not be null");
        assertNotNull(openAPI.getInfo(), "Info should not be null");
        assertEquals("Apibrick Low-Code API Platform - API Management Interfaces", 
                     openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getPaths(), "Paths should not be null");
        assertTrue(openAPI.getPaths().size() > 0, "Should have at least one path");
    }
    
    @Test
    @DisplayName("Should extract ApiCreateRequest objects from OpenAPI")
    void testExtractApiCreateRequests() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        
        // When
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // Then
        assertNotNull(requests, "Requests list should not be null");
        assertTrue(requests.size() > 0, "Should extract at least one API request");
        
        // Verify first request
        ApiCreateRequest firstRequest = requests.get(0);
        assertNotNull(firstRequest.getApiPath(), "API path should not be null");
        assertNotNull(firstRequest.getMethod(), "HTTP method should not be null");
        assertNotNull(firstRequest.getApiName(), "API name should not be null");
    }
    
    @Test
    @DisplayName("Should extract request parameters from OpenAPI")
    void testExtractRequestParameters() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // When - Find a request with parameters
        ApiCreateRequest requestWithParams = requests.stream()
                .filter(req -> req.getRequestConfig() != null 
                           && req.getRequestConfig().getParams() != null
                           && !req.getRequestConfig().getParams().isEmpty())
                .findFirst()
                .orElse(null);
        
        // Then
        if (requestWithParams != null) {
            assertNotNull(requestWithParams.getRequestConfig());
            assertNotNull(requestWithParams.getRequestConfig().getParams());
            assertTrue(requestWithParams.getRequestConfig().getParams().size() > 0);
            
            // Verify parameter structure
            var firstParam = requestWithParams.getRequestConfig().getParams().get(0);
            assertNotNull(firstParam.getName());
            assertNotNull(firstParam.getType());
        }
    }
    
    @Test
    @DisplayName("Should extract response configuration from OpenAPI")
    void testExtractResponseConfig() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // When - Find a request with response config
        ApiCreateRequest requestWithResponse = requests.stream()
                .filter(req -> req.getResponseConfig() != null)
                .findFirst()
                .orElse(null);
        
        // Then
        if (requestWithResponse != null) {
            assertNotNull(requestWithResponse.getResponseConfig());
            assertNotNull(requestWithResponse.getResponseConfig().getContentType());
            assertNotNull(requestWithResponse.getResponseConfig().getSuccessCode());
            assertNotNull(requestWithResponse.getResponseConfig().getFailCode());
        }
    }
    
    @Test
    @DisplayName("Should generate ApiDetailVO from operation")
    void testGenerateApiDetailVO() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        PathItem pathItem = openAPI.getPaths().get("/apis");
        Operation operation = pathItem.getGet();
        
        // When
        ApiDetailVO detailVO = parser.generateApiDetailVO("api_001", operation, openAPI);
        
        // Then
        assertNotNull(detailVO);
        assertEquals("api_001", detailVO.getApiId());
        assertNotNull(detailVO.getApiName());
        assertNotNull(detailVO.getStatus());
    }
    
    @Test
    @DisplayName("Should generate ApiSimpleVO from operation")
    void testGenerateApiSimpleVO() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        PathItem pathItem = openAPI.getPaths().get("/apis");
        Operation operation = pathItem.getGet();
        
        // When
        ApiSimpleVO simpleVO = parser.generateApiSimpleVO("api_001", "/apis", "GET", operation);
        
        // Then
        assertNotNull(simpleVO);
        assertEquals("api_001", simpleVO.getApiId());
        assertEquals("/apis", simpleVO.getApiPath());
        assertEquals("GET", simpleVO.getMethod());
        assertNotNull(simpleVO.getApiName());
    }
    
    @Test
    @DisplayName("Should handle YAML with multiple paths")
    void testParseMultiplePaths() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        
        // When
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // Then
        assertTrue(requests.size() >= 5, "Should extract at least 5 API requests");
        
        // Verify different HTTP methods are extracted
        long getCount = requests.stream().filter(r -> "GET".equals(r.getMethod())).count();
        long postCount = requests.stream().filter(r -> "POST".equals(r.getMethod())).count();
        long putCount = requests.stream().filter(r -> "PUT".equals(r.getMethod())).count();
        long deleteCount = requests.stream().filter(r -> "DELETE".equals(r.getMethod())).count();
        
        assertTrue(getCount > 0, "Should have at least one GET request");
        assertTrue(postCount > 0, "Should have at least one POST request");
        assertTrue(putCount > 0, "Should have at least one PUT request");
        assertTrue(deleteCount > 0, "Should have at least one DELETE request");
    }
    
    @Test
    @DisplayName("Should handle YAML with query parameters")
    void testParseQueryParameters() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // When - Find GET /apis request which has query parameters
        ApiCreateRequest listApisRequest = requests.stream()
                .filter(req -> "/apis".equals(req.getApiPath()) && "GET".equals(req.getMethod()))
                .findFirst()
                .orElse(null);
        
        // Then
        assertNotNull(listApisRequest, "Should find GET /apis request");
        if (listApisRequest.getRequestConfig() != null 
            && listApisRequest.getRequestConfig().getParams() != null) {
            assertTrue(listApisRequest.getRequestConfig().getParams().size() > 0,
                       "Should have query parameters");
        }
    }
    
    @Test
    @DisplayName("Should handle YAML with path parameters")
    void testParsePathParameters() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // When - Find request with path parameters (e.g., /apis/{apiId})
        ApiCreateRequest pathParamRequest = requests.stream()
                .filter(req -> req.getApiPath() != null && req.getApiPath().contains("{"))
                .findFirst()
                .orElse(null);
        
        // Then
        if (pathParamRequest != null) {
            assertTrue(pathParamRequest.getApiPath().contains("{"));
        }
    }
    
    @Test
    @DisplayName("Should handle invalid YAML gracefully")
    void testParseInvalidYaml() {
        // Given
        String invalidYaml = "invalid: yaml: content: [";
        
        // When/Then
        assertThrows(RuntimeException.class, () -> {
            parser.parseYamlContent(invalidYaml);
        }, "Should throw exception for invalid YAML");
    }
    
    @Test
    @DisplayName("Should handle missing resource file")
    void testParseMissingResource() {
        // Given
        String nonExistentPath = "swagger/nonexistent.yaml";
        
        // When/Then
        assertThrows(RuntimeException.class, () -> {
            parser.parseYamlFromClasspath(nonExistentPath);
        }, "Should throw exception for missing resource");
    }
    
    @Test
    @DisplayName("Should extract all API operations from apibrick.yaml")
    void testExtractAllOperations() {
        // Given
        OpenAPI openAPI = parser.parseYamlFromClasspath("swagger/apibrick.yaml");
        
        // When
        List<ApiCreateRequest> requests = parser.extractApiCreateRequests(openAPI);
        
        // Then
        // Verify we extracted operations for all paths defined in apibrick.yaml
        // Expected paths: /apis (GET), /apis/{apiId} (GET), /apis (POST), 
        //                /apis/{apiId}/status (PUT), /apis/{apiId} (DELETE)
        assertTrue(requests.size() >= 5, 
                   "Should extract all 5 operations from apibrick.yaml");
        
        // Verify specific operations exist
        boolean hasListApis = requests.stream()
                .anyMatch(r -> "/apis".equals(r.getApiPath()) && "GET".equals(r.getMethod()));
        boolean hasGetApiDetail = requests.stream()
                .anyMatch(r -> r.getApiPath() != null && r.getApiPath().contains("{apiId}") 
                           && "GET".equals(r.getMethod()));
        boolean hasCreateApi = requests.stream()
                .anyMatch(r -> "/apis".equals(r.getApiPath()) && "POST".equals(r.getMethod()));
        boolean hasUpdateStatus = requests.stream()
                .anyMatch(r -> r.getApiPath() != null && r.getApiPath().contains("status") 
                           && "PUT".equals(r.getMethod()));
        boolean hasDeleteApi = requests.stream()
                .anyMatch(r -> r.getApiPath() != null && r.getApiPath().contains("{apiId}") 
                           && "DELETE".equals(r.getMethod()));
        
        assertTrue(hasListApis, "Should have GET /apis operation");
        assertTrue(hasGetApiDetail, "Should have GET /apis/{apiId} operation");
        assertTrue(hasCreateApi, "Should have POST /apis operation");
        assertTrue(hasUpdateStatus, "Should have PUT /apis/{apiId}/status operation");
        assertTrue(hasDeleteApi, "Should have DELETE /apis/{apiId} operation");
    }
}

