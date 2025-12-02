package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.common.enums.FailureStrategy;
import com.citi.tts.apibrick.common.util.JsonUtil;
import com.citi.tts.apibrick.core.workflow.engine.FlowDefinition;
import com.citi.tts.apibrick.core.workflow.engine.PipelineEngine;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepDefinition;
import com.citi.tts.apibrick.core.workflow.engine.WorkflowResult;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import com.citi.tts.apibrick.service.domain.ApiExecuteFlow;
import com.citi.tts.apibrick.service.repository.ApiExecuteFlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API Execution Service
 * 
 * Coordinates the execution of API workflows:
 * 1. Loads ApiExecuteFlow based on flowCode
 * 2. Converts ApiExecuteFlow to FlowDefinition
 * 3. Builds StepContext with request parameters
 * 4. Executes workflow via PipelineEngine
 * 5. Handles encryption/decryption if configured
 */
@Service
public class ApiExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiExecutionService.class);
    
    @Autowired
    private ApiExecuteFlowRepository flowRepository;
    
    @Autowired
    private PipelineEngine pipelineEngine;
    
    @Autowired
    private ApiCacheManager cacheManager;
    
    /**
     * Execute API workflow
     * 
     * @param apiDef ApiDefinition
     * @param requestParams Request parameters
     * @param tenantId Tenant ID
     * @param env Environment
     * @return Mono<WorkflowResult> execution result
     */
    public Mono<WorkflowResult> executeApi(ApiDefinition apiDef, Map<String, Object> requestParams, 
                                           String tenantId, String env) {
        logger.info("Executing API: apiCode={}, flowCode={}, tenantId={}", 
                   apiDef.getApiCode(), apiDef.getFlowCode(), tenantId);
        
        // Load flow definition
        return loadFlowDefinition(apiDef.getFlowCode(), tenantId)
                .flatMap(flowDef -> {
                    // Build execution context
                    String flowId = apiDef.getFlowCode();
                    String flowInstanceId = UUID.randomUUID().toString();
                    StepContext context = new StepContext(
                            flowId,
                            flowInstanceId,
                            tenantId,
                            env,
                            requestParams
                    );
                    
                    // Execute workflow
                    return pipelineEngine.execute(flowId, flowDef, context)
                            .doOnSuccess(result -> {
                                logger.info("API execution completed: apiCode={}, success={}, time={}ms",
                                           apiDef.getApiCode(), result.isSuccess(), result.getExecuteTime());
                            })
                            .doOnError(error -> {
                                logger.error("API execution failed: apiCode={}", apiDef.getApiCode(), error);
                            });
                });
    }
    
    /**
     * Load FlowDefinition from ApiExecuteFlow
     * 
     * @param flowCode Flow code
     * @param tenantId Tenant ID
     * @return Mono<FlowDefinition>
     */
    public Mono<FlowDefinition> loadFlowDefinition(String flowCode, String tenantId) {
        // Try cache first
        return Mono.fromCallable(() -> {
            return cacheManager.getFlow(tenantId, flowCode);
        })
        .flatMap(cached -> {
            if (cached.isPresent()) {
                logger.debug("Flow loaded from cache: {}", flowCode);
                return Mono.just(convertToFlowDefinition(cached.get()));
            }
            
            // Load from database
            return Mono.fromCallable(() -> {
                return flowRepository.findByTenantIdAndFlowCode(tenantId, flowCode);
            })
//            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .flatMap(optional -> {
                if (optional.isEmpty()) {
                    return Mono.error(new IllegalArgumentException(
                            "Flow not found: flowCode=" + flowCode + ", tenantId=" + tenantId));
                }
                
                ApiExecuteFlow flow = optional.get();
                // Cache it
                cacheManager.putFlow(tenantId, flowCode, flow);
                
                return Mono.just(convertToFlowDefinition(flow));
            });
        });
    }
    
    /**
     * Convert ApiExecuteFlow to FlowDefinition
     * 
     * @param flow ApiExecuteFlow from database
     * @return FlowDefinition for execution
     */
    private FlowDefinition convertToFlowDefinition(ApiExecuteFlow flow) {
        FlowDefinition flowDef = new FlowDefinition();
        flowDef.setId(flow.getFlowCode());
        flowDef.setName(flow.getFlowName());
        flowDef.setDescription(flow.getFlowDesc());
        flowDef.setExecutionMode(FlowDefinition.ExecutionMode.SEQUENTIAL);
        
        // Convert stepList to StepDefinition list
        List<StepDefinition> stepDefinitions = new ArrayList<>();
        
        // The stepList is stored as JSON, need to parse it
        // Based on firstFlow.json structure: stepList contains objects with stepId, stepType, stepConfig, etc.
        Object stepListObj = flow.getStepList();
        
        if (stepListObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepList = (List<Map<String, Object>>) stepListObj;
            
            for (Map<String, Object> stepMap : stepList) {
                StepDefinition stepDef = new StepDefinition();
                
                // Extract stepId (could be stepId or id)
                String stepId = (String) stepMap.get("stepId");
                if (stepId == null) {
                    stepId = (String) stepMap.get("id");
                }
                stepDef.setId(stepId);
                
                // Extract stepType (could be stepType or type)
                String stepType = (String) stepMap.get("stepType");
                if (stepType == null) {
                    stepType = (String) stepMap.get("type");
                }
                stepDef.setType(stepType);
                
                // Extract stepConfig (could be stepConfig or config)
                @SuppressWarnings("unchecked")
                Map<String, Object> stepConfig = (Map<String, Object>) stepMap.get("stepConfig");
                if (stepConfig == null) {
                    stepConfig = (Map<String, Object>) stepMap.get("config");
                }
                stepDef.setConfig(stepConfig);
                
                // Extract failure strategy
                Boolean failStop = (Boolean) stepMap.get("failStop");
                if (failStop != null && failStop) {
                    stepDef.setFailureStrategy(FailureStrategy.TERMINATE);
        } else {
            stepDef.setFailureStrategy(FailureStrategy.TERMINATE);
        }
                
                stepDefinitions.add(stepDef);
            }
        } else if (stepListObj != null) {
            // Try to parse as JSON string if it's a string
            if (stepListObj instanceof String) {
                try {
                    List<Map<String, Object>> stepList = JsonUtil.parse((String) stepListObj, List.class);
                    for (Map<String, Object> stepMap : stepList) {
                        StepDefinition stepDef = convertStepMapToDefinition(stepMap);
                        stepDefinitions.add(stepDef);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse stepList JSON: {}", e.getMessage(), e);
                }
            }
        }
        
        flowDef.setSteps(stepDefinitions);
        logger.debug("Converted flow: flowCode={}, steps={}", flow.getFlowCode(), stepDefinitions.size());
        
        return flowDef;
    }
    
    /**
     * Convert step map to StepDefinition
     */
    private StepDefinition convertStepMapToDefinition(Map<String, Object> stepMap) {
        StepDefinition stepDef = new StepDefinition();
        
        String stepId = (String) stepMap.get("stepId");
        if (stepId == null) {
            stepId = (String) stepMap.get("id");
        }
        stepDef.setId(stepId);
        
        String stepType = (String) stepMap.get("stepType");
        if (stepType == null) {
            stepType = (String) stepMap.get("type");
        }
        stepDef.setType(stepType);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> stepConfig = (Map<String, Object>) stepMap.get("stepConfig");
        if (stepConfig == null) {
            stepConfig = (Map<String, Object>) stepMap.get("config");
        }
        stepDef.setConfig(stepConfig);
        
        Boolean failStop = (Boolean) stepMap.get("failStop");
        if (failStop != null && failStop) {
            stepDef.setFailureStrategy(FailureStrategy.TERMINATE);
        } else {
            stepDef.setFailureStrategy(FailureStrategy.SKIP);
        }
        
        return stepDef;
    }
}

