package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.response.ResponseGenerator;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import com.citi.tts.apibrick.core.workflow.steps.ResponseStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static reactor.core.publisher.Mono.when;

/**
 * Unit tests for ResponseStep
 */
@ExtendWith(MockitoExtension.class)
class ResponseStepTest {
    
    @Mock
    private ResponseGenerator responseGenerator;
    
    @InjectMocks
    private ResponseStep responseStep;
    
    private StepContext context;
    
    @BeforeEach
    void setUp() {
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("customerId", "123456");
        
        context = new StepContext(
            "test-flow",
            "instance-001",
            "tenant-001",
            "DEV",
            requestParams
        );
    }
    
    @Test
    void testExecute_WithResponseTemplate() {
        // Given: Response template configuration
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("code", 200);
        responseTemplate.put("message", "success");
        
        context.set("responseTemplate", responseTemplate);
        
        // Mock response generator
        Map<String, Object> generatedResponse = new HashMap<>();
        generatedResponse.put("code", 200);
        generatedResponse.put("message", "success");
        generatedResponse.put("data", Map.of("id", "123456", "name", "张三"));
        
        when((Publisher<?>) responseGenerator.generate(eq(responseTemplate), any(StepContext.class)))
            .thenReturn(generatedResponse);
        
        // When: Execute step
        Mono<StepResult> resultMono = responseStep.execute(context);
        
        // Then: Verify result
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertTrue(result.isSuccess());
                assertNotNull(result.getOutput());
                assertNotNull(result.getOutput().get("response"));
                assertEquals("RESPONSE", result.getStepType());
                assertTrue(result.getExecuteTime() >= 0);
            })
            .verifyComplete();
        
        // Verify response was stored in context
        assertNotNull(context.get("finalResponse"));
        
        // Verify response generator was called
        verify(responseGenerator, times(1)).generate(eq(responseTemplate), any(StepContext.class));
    }
    
    @Test
    void testExecute_WithConditionalTemplate() {
        // Given: Conditional response template
        Map<String, Object> responseTemplate = new HashMap<>();
        
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("sourceType", "expression");
        codeField.put("sourceValue", "${steps.query.output != null ? 200 : 404}");
        responseTemplate.put("code", codeField);
        
        Map<String, Object> messageField = new HashMap<>();
        messageField.put("sourceType", "expression");
        messageField.put("sourceValue", "${steps.query.output != null ? 'success' : '客户不存在'}");
        responseTemplate.put("message", messageField);
        
        context.set("responseTemplate", responseTemplate);
        context.set("query.output", null); // Customer not found
        
        // Mock response generator
        Map<String, Object> generatedResponse = new HashMap<>();
        generatedResponse.put("code", 404);
        generatedResponse.put("message", "客户不存在");
        
        when((Publisher<?>) responseGenerator.generate(eq(responseTemplate), any(StepContext.class)))
            .thenReturn(generatedResponse);
        
        // When: Execute step
        Mono<StepResult> resultMono = responseStep.execute(context);
        
        // Then: Verify 404 response
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertTrue(result.isSuccess());
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) result.getOutput().get("response");
                assertEquals(404, response.get("code"));
                assertEquals("客户不存在", response.get("message"));
            })
            .verifyComplete();
    }
    
    @Test
    void testGetType() {
        // When/Then: Verify step type
        assertEquals("RESPONSE", responseStep.getType());
    }
}

