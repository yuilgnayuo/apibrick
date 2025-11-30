package com.citi.tts.apibrick.core.workflow.engine;

import com.citi.tts.apibrick.core.workflow.engine.*;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for PipelineEngine
 * Tests workflow execution with DATASOURCE_QUERY and RESPONSE steps
 */
@ExtendWith(MockitoExtension.class)
class PipelineEngineTest {

    private PipelineEngine pipelineEngine;
    private StepRegistry stepRegistry;

    @Mock
    private Step mockDataSourceStep;

    @Mock
    private Step mockResponseStep;

    @BeforeEach
    void setUp() {
        stepRegistry = new StepRegistry();

        // Register mock steps
        when(mockDataSourceStep.getType()).thenReturn("DATASOURCE_QUERY");
        when(mockResponseStep.getType()).thenReturn("RESPONSE");

        stepRegistry.registerStep(mockDataSourceStep);
        stepRegistry.registerStep(mockResponseStep);

        pipelineEngine = new PipelineEngine(stepRegistry);
    }

    @Test
    void testWorkflowExecution_Success() {
        // Given: Flow definition with query and response steps
        FlowDefinition flowDefinition = createFlowDefinition();
        StepContext context = createStepContext("123456");

        // Mock data source query step - returns customer data
        Map<String, Object> queryOutput = new HashMap<>();
        queryOutput.put("id", "123456");
        queryOutput.put("name", "张三");
        queryOutput.put("phone", "13800138000");
        queryOutput.put("balance", 15000);

        StepResult queryResult = StepResult.success(
                Map.of("data", queryOutput),
                100
        );
        queryResult.setStepId("query");
        queryResult.setStepType("DATASOURCE_QUERY");
        doReturn(Mono.just(queryResult))
                .when(mockDataSourceStep)
                .execute(any(StepContext.class));

        // Mock response step - generates final response
        Map<String, Object> responseOutput = new HashMap<>();
        responseOutput.put("code", 200);
        responseOutput.put("message", "success");
        responseOutput.put("data", queryOutput);

        StepResult responseResult = StepResult.success(
                Map.of("response", responseOutput),
                50
        );
        responseResult.setStepId("response");
        responseResult.setStepType("RESPONSE");

        doReturn(Mono.just(responseResult))
                .when(mockResponseStep)
                .execute(any(StepContext.class));

        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
                "test-flow",
                flowDefinition,
                context
        );
        // Then: Verify successful execution
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertNotNull(result.getOutput());
                    assertNotNull(result.getOutput().get("data"));
                    assertTrue(result.getExecuteTime() > 0);
                    assertNotNull(result.getFlowInstanceId());
                })
                .verifyComplete();
    }

    @Test
    void testWorkflowExecution_CustomerNotFound() {
        // Given: Flow definition with query and response steps
        FlowDefinition flowDefinition = createFlowDefinition();
        StepContext context = createStepContext("999999");

        // Mock data source query step - returns null (customer not found)
        StepResult queryResult = StepResult.success(
                Map.of("data", null),
                100
        );
        queryResult.setStepId("query");
        queryResult.setStepType("DATASOURCE_QUERY");

        when(mockDataSourceStep.execute(any(StepContext.class)))
                .thenReturn(Mono.just(queryResult));

        // Mock response step - generates 404 response
        Map<String, Object> responseOutput = new HashMap<>();
        responseOutput.put("code", 404);
        responseOutput.put("message", "客户不存在");
        responseOutput.put("data", null);

        StepResult responseResult = StepResult.success(
                Map.of("response", responseOutput),
                50
        );
        responseResult.setStepId("response");
        responseResult.setStepType("RESPONSE");

        when(mockResponseStep.execute(any(StepContext.class)))
                .thenReturn(Mono.just(responseResult));

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
                    assertEquals(404, result.getOutput().get("code"));
                    assertEquals("客户不存在", result.getOutput().get("message"));
                    assertNull(result.getOutput().get("data"));
                })
                .verifyComplete();
    }

    @Test
    void testWorkflowExecution_QueryStepFails() {
        // Given: Flow definition with query and response steps
        FlowDefinition flowDefinition = createFlowDefinition();
        StepContext context = createStepContext("123456");

        // Mock data source query step - fails
        StepResult queryResult = StepResult.failure(
                "Database connection error",
                100
        );
        queryResult.setStepId("query");
        queryResult.setStepType("DATASOURCE_QUERY");

        when(mockDataSourceStep.execute(any(StepContext.class)))
                .thenReturn(Mono.just(queryResult));

        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
                "test-flow",
                flowDefinition,
                context
        );

        // Then: Verify workflow fails
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertEquals("Database connection error", result.getErrorMsg());
                })
                .verifyComplete();

        // Verify response step was not called (terminated on failure)
        verify(mockResponseStep, never()).execute(any(StepContext.class));
    }

    @Test
    void testWorkflowExecution_EmptySteps() {
        // Given: Flow definition with no steps
        FlowDefinition flowDefinition = new FlowDefinition();
        flowDefinition.setId("empty-flow");
        flowDefinition.setSteps(List.of());
        StepContext context = createStepContext("123456");

        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
                "empty-flow",
                flowDefinition,
                context
        );

        // Then: Verify failure
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertTrue(result.getErrorMsg().contains("No steps"));
                })
                .verifyComplete();
    }

    @Test
    void testWorkflowExecution_StepNotFound() {
        // Given: Flow definition with unknown step type
        FlowDefinition flowDefinition = new FlowDefinition();
        flowDefinition.setId("test-flow");

        StepDefinition unknownStep = new StepDefinition();
        unknownStep.setId("unknown");
        unknownStep.setType("UNKNOWN_STEP");
        unknownStep.setConfig(new HashMap<>());

        flowDefinition.setSteps(List.of(unknownStep));
        StepContext context = createStepContext("123456");

        // When: Execute workflow
        Mono<WorkflowResult> resultMono = pipelineEngine.execute(
                "test-flow",
                flowDefinition,
                context
        );

        // Then: Verify failure
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertTrue(result.getErrorMsg().contains("not found"));
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

