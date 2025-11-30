package com.citi.tts.apibrick.core.response;

import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResponseGenerator
 * Tests dynamic response generation based on templates and context
 */
class ResponseGeneratorTest {
    
    private ResponseGenerator responseGenerator;
    
    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
    }
    
    @Test
    void testGenerateResponse_WithStepOutput() {
        // Given: Step context with query result
        StepContext context = createContextWithQueryResult();
        
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("code", 200);
        responseTemplate.put("message", "success");
        responseTemplate.put("data", "${steps.query.output}");
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify response
        assertEquals(200, response.get("code"));
        assertEquals("success", response.get("message"));
        assertNotNull(response.get("data"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("123456", data.get("id"));
        assertEquals("张三", data.get("name"));
    }
    
    @Test
    void testGenerateResponse_WithConditionalExpression() {
        // Given: Step context with query result
        StepContext context = createContextWithQueryResult();
        
        Map<String, Object> responseTemplate = new HashMap<>();
        
        // Conditional code
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("sourceType", "expression");
        codeField.put("sourceValue", "${steps.query.output != null ? 200 : 404}");
        responseTemplate.put("code", codeField);
        
        // Conditional message
        Map<String, Object> messageField = new HashMap<>();
        messageField.put("sourceType", "expression");
        messageField.put("sourceValue", "${steps.query.output != null ? 'success' : '客户不存在'}");
        responseTemplate.put("message", messageField);
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify conditional response
        assertEquals(200, response.get("code"));
        assertEquals("success", response.get("message"));
    }
    
    @Test
    void testGenerateResponse_CustomerNotFound() {
        // Given: Step context with null query result
        StepContext context = createContextWithNullQueryResult();
        
        Map<String, Object> responseTemplate = new HashMap<>();
        
        // Conditional code
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("sourceType", "expression");
        codeField.put("sourceValue", "${steps.query.output != null ? 200 : 404}");
        responseTemplate.put("code", codeField);
        
        // Conditional message
        Map<String, Object> messageField = new HashMap<>();
        messageField.put("sourceType", "expression");
        messageField.put("sourceValue", "${steps.query.output != null ? 'success' : '客户不存在'}");
        responseTemplate.put("message", messageField);
        
        // Data field with condition
        Map<String, Object> dataField = new HashMap<>();
        dataField.put("sourceType", "stepOutput");
        dataField.put("sourceValue", "query.output");
        dataField.put("defaultValue", null);
        dataField.put("condition", "${steps.query.output != null}");
        responseTemplate.put("data", dataField);
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify 404 response
        assertEquals(404, response.get("code"));
        assertEquals("客户不存在", response.get("message"));
        assertNull(response.get("data"));
    }
    
    @Test
    void testGenerateResponse_WithFieldMapping() {
        // Given: Step context with query result
        StepContext context = createContextWithQueryResult();
        
        Map<String, Object> responseTemplate = new HashMap<>();
        
        // Fixed code
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("sourceType", "fixed");
        codeField.put("sourceValue", 200);
        responseTemplate.put("code", codeField);
        
        // Step output mapping
        Map<String, Object> dataField = new HashMap<>();
        dataField.put("sourceType", "stepOutput");
        dataField.put("sourceValue", "query.output");
        responseTemplate.put("data", dataField);
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify response
        assertEquals(200, response.get("code"));
        assertNotNull(response.get("data"));
    }
    
    @Test
    void testGenerateResponse_WithRequestParam() {
        // Given: Step context with request parameter
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("customerId", "123456");
        
        StepContext context = new StepContext(
            "test-flow",
            "instance-001",
            "tenant-001",
            "DEV",
            requestParams
        );
        
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("customerId", "${request.customerId}");
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify request parameter is extracted
        assertEquals("123456", response.get("customerId"));
    }
    
    @Test
    void testGenerateResponse_WithNestedStructure() {
        // Given: Step context with query result
        StepContext context = createContextWithQueryResult();
        
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("code", 200);
        
        // Nested data structure
        Map<String, Object> dataTemplate = new HashMap<>();
        dataTemplate.put("customerId", "${steps.query.output.id}");
        dataTemplate.put("customerName", "${steps.query.output.name}");
        dataTemplate.put("phone", "${steps.query.output.phone}");
        
        responseTemplate.put("data", dataTemplate);
        
        // When: Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Then: Verify nested structure
        assertEquals(200, response.get("code"));
        assertNotNull(response.get("data"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("123456", data.get("customerId"));
        assertEquals("张三", data.get("customerName"));
        assertEquals("13800138000", data.get("phone"));
    }
    
    @Test
    void testGenerateResponse_DefaultResponse() {
        // Given: Step context without template
        StepContext context = createContextWithQueryResult();
        
        // When: Generate response with null template
        Map<String, Object> response = responseGenerator.generate(null, context);
        
        // Then: Verify default response
        assertEquals(200, response.get("code"));
        assertEquals("success", response.get("message"));
        assertNotNull(response.get("data"));
    }
    
    /**
     * Create context with query result
     */
    private StepContext createContextWithQueryResult() {
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("customerId", "123456");
        
        StepContext context = new StepContext(
            "test-flow",
            "instance-001",
            "tenant-001",
            "DEV",
            requestParams
        );
        
        // Add query step output
        Map<String, Object> queryOutput = new HashMap<>();
        queryOutput.put("id", "123456");
        queryOutput.put("name", "张三");
        queryOutput.put("phone", "13800138000");
        queryOutput.put("balance", 15000);
        
        context.set("query.output", queryOutput);
        
        return context;
    }
    
    /**
     * Create context with null query result (customer not found)
     */
    private StepContext createContextWithNullQueryResult() {
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("customerId", "999999");
        
        StepContext context = new StepContext(
            "test-flow",
            "instance-001",
            "tenant-001",
            "DEV",
            requestParams
        );
        
        // Add null query result
        context.set("query.output", null);
        
        return context;
    }
}

