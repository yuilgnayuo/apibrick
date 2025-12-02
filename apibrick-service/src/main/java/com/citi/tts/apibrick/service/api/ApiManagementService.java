package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.core.workflow.engine.FlowDefinition;
import com.citi.tts.apibrick.core.workflow.engine.PipelineEngine;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.WorkflowResult;
import com.citi.tts.apibrick.model.api.APIEndpoint;
import com.citi.tts.apibrick.model.swagger.SwaggerParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

/**
 * API Management Service
 * 
 * Manages API endpoints, flow definitions, and execution
 * Integrates with Swagger parser for API import
 */
@Service
public class ApiManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiManagementService.class);
    
    private SwaggerParserService swaggerParserService;
    
    @Autowired
    private PipelineEngine pipelineEngine;
    
//    @Autowired
//    private MonitoringService monitoringService;
    
    /**
     * Import APIs from Swagger/OpenAPI specification
     * 
     * @param swaggerJson Swagger JSON content
     * @return List of parsed API endpoints
     */
    public Mono<List<APIEndpoint>> importFromSwagger(String swaggerJson) {
        return Mono.fromCallable(() -> {
            return swaggerParserService.parseSwaggerJson(swaggerJson);
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .doOnSuccess(endpoints -> {
            logger.info("Imported {} APIs from Swagger", endpoints.size());
        })
        .doOnError(error -> {
            logger.error("Failed to import APIs from Swagger", error);
        });
    }
    
    /**
     * Execute a workflow pipeline
     * 
     * @param flowId Flow definition ID
     * @param flowDefinition Flow definition
     * @param requestParams Request parameters
     * @param tenantId Tenant ID
     * @param env Environment
     * @return Workflow execution result
     */
    public Mono<WorkflowResult> executeWorkflow(
            String flowId,
            FlowDefinition flowDefinition,
            Map<String, Object> requestParams,
            String tenantId,
            String env) {
        
        long startTime = System.currentTimeMillis();
        StepContext context = new StepContext(flowId, null, tenantId, env, requestParams);
        
        return pipelineEngine.execute(flowId, flowDefinition, context)
            .doOnSuccess(result -> {
                long executeTime = System.currentTimeMillis() - startTime;
//                monitoringService.recordWorkflowExecution(flowId, tenantId, result.isSuccess(), executeTime);
                logger.info("Workflow executed. flowId={}, success={}, time={}ms",
                           flowId, result.isSuccess(), executeTime);
            })
            .doOnError(error -> {
                long executeTime = System.currentTimeMillis() - startTime;
//                monitoringService.recordWorkflowExecution(flowId, tenantId, false, executeTime);
                logger.error("Workflow execution error. flowId={}", flowId, error);
            });
    }
    
    /**
     * Validate flow definition
     * 
     * @param flowDefinition Flow definition to validate
     * @return Mono<Boolean> true if valid
     */
    public Mono<Boolean> validateFlowDefinition(FlowDefinition flowDefinition) {
        return Mono.fromCallable(() -> {
            if (flowDefinition == null) {
                return false;
            }
            if (flowDefinition.getId() == null || flowDefinition.getId().isEmpty()) {
                return false;
            }
            if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
                return false;
            }
            return true;
        });
    }
}
