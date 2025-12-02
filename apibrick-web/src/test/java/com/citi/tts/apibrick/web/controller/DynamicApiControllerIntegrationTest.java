package com.citi.tts.apibrick.web.controller;

import com.citi.tts.apibrick.common.enums.ApiStatus;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import com.citi.tts.apibrick.service.domain.ApiExecuteFlow;
import com.citi.tts.apibrick.service.repository.ApiDefinitionRepository;
import com.citi.tts.apibrick.service.repository.ApiExecuteFlowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration test for DynamicApiController
 * 
 * Tests the complete request processing flow:
 * 1. Request received
 * 2. API matched
 * 3. Parameters resolved
 * 4. Workflow executed
 * 5. Response formatted and returned
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DynamicApiControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ApiDefinitionRepository apiDefinitionRepository;
    
    @Autowired
    private ApiExecuteFlowRepository flowRepository;
    
    private static final String TEST_TENANT_ID = "test-tenant-001";
    private static final String TEST_API_PATH = "/api/v1/test/user";
    private static final String TEST_FLOW_CODE = "flow_test_001";
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        apiDefinitionRepository.deleteAll();
        flowRepository.deleteAll();
        
        // Create test ApiDefinition
        ApiDefinition apiDef = new ApiDefinition();
        apiDef.setTenantId(TEST_TENANT_ID);
        apiDef.setApiCode("test_api_001");
        apiDef.setApiName("Test User API");
        apiDef.setApiPath(TEST_API_PATH);
        apiDef.setHttpMethod("GET");
        apiDef.setApiStatus(ApiStatus.ENABLED);
        apiDef.setFlowCode(TEST_FLOW_CODE);
        
        // Set request params
        List<Map<String, Object>> requestParams = new ArrayList<>();
        Map<String, Object> param = new HashMap<>();
        param.put("name", "userId");
        param.put("in", "query");
        param.put("required", false);
        param.put("type", "string");
        requestParams.add(param);
        apiDef.setRequestParams(requestParams);
        
        // Set response def
        Map<Integer, Map<String, Object>> responseDef = new HashMap<>();
        Map<String, Object> response200 = new HashMap<>();
        response200.put("description", "Success");
        responseDef.put(200, response200);
        apiDef.setResponseDef(responseDef);
        
        apiDefinitionRepository.save(apiDef);
        
        // Create test ApiExecuteFlow
        ApiExecuteFlow flow = new ApiExecuteFlow();
        flow.setTenantId(TEST_TENANT_ID);
        flow.setFlowCode(TEST_FLOW_CODE);
        flow.setFlowName("Test Flow");
        
        // Create step list
        List<Map<String, Object>> stepList = new ArrayList<>();
        
        // Step 1: Response step
        Map<String, Object> responseStep = new HashMap<>();
        responseStep.put("stepId", "step_response");
        responseStep.put("stepType", "RESPONSE");
        Map<String, Object> responseConfig = new HashMap<>();
        Map<String, Object> responseTemplate = new HashMap<>();
        responseTemplate.put("code", Map.of("sourceType", "constant", "sourceValue", 200));
        responseTemplate.put("msg", Map.of("sourceType", "constant", "sourceValue", "success"));
        responseTemplate.put("data", Map.of("sourceType", "stepOutput", "sourceValue", "step_response.output"));
        responseConfig.put("responseTemplate", responseTemplate);
        responseStep.put("stepConfig", responseConfig);
        responseStep.put("failStop", true);

        stepList.add(responseStep);
        
        flow.setStepList(stepList);
        flowRepository.save(flow);
    }
    
    @Test
    void testDynamicApiRequest_Success() {
        webTestClient.get()
                .uri(TEST_API_PATH + "?tenantId=" + TEST_TENANT_ID)
                .header("X-Tenant-Id", TEST_TENANT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").exists()
                .jsonPath("$.msg").exists();
    }
    
    @Test
    void testDynamicApiRequest_NotFound() {
        webTestClient.get()
                .uri("/api/v1/nonexistent?tenantId=" + TEST_TENANT_ID)
                .header("X-Tenant-Id", TEST_TENANT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk() // Returns 200 with error body
                .expectBody()
                .jsonPath("$.code").isEqualTo(404)
                .jsonPath("$.msg").isEqualTo("API not found");
    }
    
    @Test
    void testDynamicApiRequest_WithPathParameter() {
        // Create API with path parameter
        ApiDefinition apiDef = new ApiDefinition();
        apiDef.setTenantId(TEST_TENANT_ID);
        apiDef.setApiCode("test_api_002");
        apiDef.setApiName("Test User Detail API");
        apiDef.setApiPath("/api/v1/test/user/{userId}");
        apiDef.setHttpMethod("GET");
        apiDef.setApiStatus(ApiStatus.ENABLED);
        apiDef.setFlowCode(TEST_FLOW_CODE);
        apiDef.setRequestParams(new ArrayList<>());
        apiDef.setResponseDef(new HashMap<>());
        apiDefinitionRepository.save(apiDef);
        
        webTestClient.get()
                .uri("/api/v1/test/user/123?tenantId=" + TEST_TENANT_ID)
                .header("X-Tenant-Id", TEST_TENANT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").exists();
    }
}

