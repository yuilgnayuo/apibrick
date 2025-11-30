package com.citi.tts.apibrick.core.workflow.integration;

import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.manager.DataSourceManager;
import com.citi.tts.apibrick.core.response.ResponseGenerator;
import com.citi.tts.apibrick.core.workflow.config.StepRegistry;
import com.citi.tts.apibrick.core.workflow.engine.*;
import com.citi.tts.apibrick.core.workflow.steps.DataSourceQueryStep;
import com.citi.tts.apibrick.core.workflow.steps.ResponseStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test for complete workflow execution
 * Tests the full flow: DATASOURCE_QUERY -> RESPONSE
 */
@ExtendWith(MockitoExtension.class)
class WorkflowIntegrationTest {
    
    private PipelineEngine pipelineEngine;
    private StepRegistry stepRegistry;
    private ResponseGenerator responseGenerator;
    
    @Mock
    private DataSourceManager dataSourceManager;
    
    @Mock
    private DataSource mockDataSource;
    
    @BeforeEach
    void setUp() {
        stepRegistry = new StepRegistry();
        responseGenerator = new ResponseGenerator();
        
        // Register real steps
        DataSourceQueryStep dataSourceStep = new DataSourceQueryStep();
        // Inject mocked DataSourceManager using reflection for testing
        try {
            var field = DataSourceQueryStep.class.getDeclaredField("dataSourceManager");
            field.setAccessible(true);
            field.set(dataSourceStep, dataSourceManager);
        } catch (Exception e) {
            // Fallback: use mock
        }
        
        ResponseStep responseStep = new ResponseStep();
        try {
            var field = ResponseStep.class.getDeclaredField("responseGenerator");
            field.setAccessible(true);
            field.set(responseStep, responseGenerator);
        } catch (Exception e) {
            // Fallback: use mock
        }
        
        stepRegistry.registerStep(dataSourceStep);
        stepRegistry.registerStep(responseStep);
        
        pipelineEngine = new PipelineEngine(stepRegistry);
    }
    
    @Test
    void testCompleteWorkflow_CustomerFound() {
        // Given: Flow definition matching the provided configuration
        FlowDefinition flowDefinition = createFlowDefinition();
        StepContext context = createStepContext("123456");
        
        // Mock data source query
        Map<String, Object> queryResult = new HashMap<>();
        queryResult.put("id", "123456");
        queryResult.put("name", "张三");
        queryResult.put("phone", "13800138000");
        queryResult.put("balance", 15000);
        
        when(dataSourceManager.getDataSource(eq("customer-db"), eq("tenant-001")))
            .thenReturn(Mono.just(mockDataSource));
        
        when(mockDataSource.executeQuery(any(Map.class)))
            .thenReturn(Mono.just(Map.of("data", queryResult)));
        
        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
            "test-flow",
            flowDefinition,
            context
        );
        
        // Then: Verify successful response
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertTrue(result.isSuccess());
                assertNotNull(result.getOutput());
                
                // Verify response structure matches template
                Object code = result.getOutput().get("code");
                Object message = result.getOutput().get("message");
                Object data = result.getOutput().get("data");
                
                // Code should be 200 (customer found)
                assertEquals(200, code);
                assertEquals("success", message);
                assertNotNull(data);
            })
            .verifyComplete();
    }
    
    @Test
    void testCompleteWorkflow_CustomerNotFound() {
        // Given: Flow definition with customer ID that doesn't exist
        FlowDefinition flowDefinition = createFlowDefinition();
        StepContext context = createStepContext("999999");
        
        // Mock data source query returning null
        when(dataSourceManager.getDataSource(eq("customer-db"), eq("tenant-001")))
            .thenReturn(Mono.just(mockDataSource));
        
        when(mockDataSource.executeQuery(any(Map.class)))
            .thenReturn(Mono.just(Map.of("data", null)));
        
        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
            "test-flow",
            flowDefinition,
            context
        );
        
        // Then: Verify 404 response
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertTrue(result.isSuccess());
                assertNotNull(result.getOutput());
                
                // Verify 404 response
                Object code = result.getOutput().get("code");
                Object message = result.getOutput().get("message");
                
                assertEquals(404, code);
                assertEquals("客户不存在", message);
            })
            .verifyComplete();
    }
    
    /**
     * Create flow definition matching the provided configuration
     */
    private FlowDefinition createFlowDefinition() {
        FlowDefinition flowDefinition = new FlowDefinition();
        flowDefinition.setId("test-flow");
        flowDefinition.setName("Query Customer API");
        
        // Step 1: DATASOURCE_QUERY
        StepDefinition queryStep = new StepDefinition();
        queryStep.setId("query");
        queryStep.setType("DATASOURCE_QUERY");
        
        Map<String, Object> queryConfig = new HashMap<>();
        queryConfig.put("datasourceId", "customer-db");
        queryConfig.put("datasourceType", "ORACLE");
        
        Map<String, Object> queryConfigInner = new HashMap<>();
        queryConfigInner.put("sql", "SELECT * FROM customer WHERE id = #{customerId}");
        queryConfig.put("queryConfig", queryConfigInner);
        
        queryStep.setConfig(queryConfig);
        queryStep.setOutputKey("query.output");
        queryStep.setFailureStrategy(FailureStrategy.TERMINATE);
        
        // Step 2: RESPONSE
        StepDefinition responseStep = new StepDefinition();
        responseStep.setId("response");
        responseStep.setType("RESPONSE");
        
        Map<String, Object> responseConfig = new HashMap<>();
        Map<String, Object> responseTemplate = new HashMap<>();
        
        // code field
        Map<String, Object> codeField = new HashMap<>();
        codeField.put("sourceType", "expression");
        codeField.put("sourceValue", "${steps.query.output != null ? 200 : 404}");
        responseTemplate.put("code", codeField);
        
        // message field
        Map<String, Object> messageField = new HashMap<>();
        messageField.put("sourceType", "expression");
        messageField.put("sourceValue", "${steps.query.output != null ? 'success' : '客户不存在'}");
        responseTemplate.put("message", messageField);
        
        // data field
        Map<String, Object> dataField = new HashMap<>();
        dataField.put("sourceType", "stepOutput");
        dataField.put("sourceValue", "query.output");
        dataField.put("defaultValue", null);
        dataField.put("condition", "${steps.query.output != null}");
        responseTemplate.put("data", dataField);
        
        responseConfig.put("responseTemplate", responseTemplate);
        responseStep.setConfig(responseConfig);
        
        flowDefinition.setSteps(List.of(queryStep, responseStep));
        
        return flowDefinition;
    }
    
    /**
     * Create step context with customer ID
     */
    private StepContext createStepContext(String customerId) {
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("customerId", customerId);
        
        return new StepContext(
            "test-flow",
            null,
            "tenant-001",
            "DEV",
            requestParams
        );
    }
}

