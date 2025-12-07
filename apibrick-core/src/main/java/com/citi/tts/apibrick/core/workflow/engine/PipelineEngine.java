package com.citi.tts.apibrick.core.workflow.engine;

import com.citi.tts.apibrick.common.enums.FailureStrategy;
import com.citi.tts.apibrick.core.workflow.config.StepRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline Engine - Core workflow execution engine
 * Executes pipeline workflows defined as sequences of steps
 * <p>
 * Supports:
 * - Sequential execution (steps executed in order)
 * - Parallel execution (independent steps executed concurrently)
 * - Branch execution (conditional routing based on step results)
 * <p>
 * All execution is reactive and non-blocking using WebFlux
 */
@Component
public class PipelineEngine {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEngine.class);

    private final StepRegistry stepRegistry;

    @Autowired
    public PipelineEngine(StepRegistry stepRegistry) {
        this.stepRegistry = stepRegistry;
    }

    /**
     * Execute a pipeline workflow
     *
     * @param flowId         Flow definition ID
     * @param flowDefinition Flow definition containing step configurations
     * @param context        Initial execution context
     * @return Mono<WorkflowResult> containing final execution result
     */
    public Mono<WorkflowResult> execute(String flowId, FlowDefinition flowDefinition, StepContext context) {

        logger.info("Executing pipeline flowId={}, tenantId={}",
                flowId, context.getTenantId());
        context.set("timestamp", System.currentTimeMillis());
        List<StepDefinition> steps = flowDefinition.getSteps();
        if (steps == null || steps.isEmpty()) {
            return Mono.just(WorkflowResult.failure("No steps defined in workflow", 0, flowId));
        }
        String flowInstanceId = context.getFlowInstanceId();
        // Execute steps sequentially
        return executeStepsSequentially(steps, context, 0)
                .collectList()
                .map(stepResults -> {
                    // Check if any step failed
                    boolean allSuccess = stepResults.stream().allMatch(StepResult::isSuccess);
                    long totalTime = stepResults.stream()
                            .mapToLong(StepResult::getExecuteTime)
                            .sum();

                    if (allSuccess) {
                        // Check if there's a ResponseStep that generated final response
                        Object finalResponse = context.get("finalResponse");
                        if (finalResponse instanceof Map) {
                            // Use the final response from ResponseStep
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMap = (Map<String, Object>) finalResponse;
                            return WorkflowResult.success(responseMap, totalTime, flowInstanceId);
                        }

                        // Otherwise, aggregate all step outputs into final result
                        Map<String, Object> finalOutput = new java.util.HashMap<>();
                        stepResults.forEach(result -> {
                            if (result.getOutput() != null) {
                                finalOutput.putAll(result.getOutput());
                            }
                        });
                        return WorkflowResult.success(finalOutput, totalTime, flowInstanceId);
                    } else {
                        // Find first failure
                        StepResult failure = stepResults.stream()
                                .filter(r -> !r.isSuccess())
                                .findFirst()
                                .orElse(null);
                        return WorkflowResult.failure(
                                failure != null ? failure.getErrorMsg() : "Unknown error",
                                totalTime,
                                flowInstanceId
                        );
                    }
                })
                .doOnSuccess(result -> {
                    if (result.isSuccess()) {
                        logger.info("Pipeline execution completed successfully. flowId={}, instanceId={}, time={}ms",
                                flowId, flowInstanceId, result.getExecuteTime());
                    } else {
                        logger.warn("Pipeline execution failed. flowId={}, instanceId={}, error={}",
                                flowId, flowInstanceId, result.getErrorMsg());
                    }
                })
                .doOnError(error -> {
                    logger.error("Pipeline execution error. flowId={}, instanceId={}",
                            flowId, flowInstanceId, error);
                });
    }

    /**
     * Execute steps sequentially
     * Each step receives the context (which may be modified by previous steps)
     *
     * @param steps      List of step definitions to execute
     * @param context    Execution context
     * @param startIndex Starting index
     * @return Flux<StepResult> stream of step execution results
     */
    private Flux<StepResult> executeStepsSequentially(
            List<StepDefinition> steps,
            StepContext context,
            int startIndex) {

        if (startIndex >= steps.size()) {
            return Flux.empty();
        }

        StepDefinition stepDef = steps.get(startIndex);
        Step step = stepRegistry.getStep(stepDef.getType());

        if (step == null) {
            logger.error("Step type '{}' not found in registry", stepDef.getType());
            return Flux.just(StepResult.failure(
                    "Step type '" + stepDef.getType() + "' not found",
                    0
            ));
        }

        long startTime = System.currentTimeMillis();

        // Store step configuration in context for step to access
        if (stepDef.getConfig() != null) {
            for (Map.Entry<String, Object> entry : stepDef.getConfig().entrySet()) {
                context.set(entry.getKey(), entry.getValue());
            }
        }

        return Mono.fromCallable(() -> {
                    // Validate step configuration
                    step.validateConfig(stepDef.getConfig());
                    return step;
                })
                .flatMap(s -> s.execute(context))
                .map(result -> {
                    result.setExecuteTime(System.currentTimeMillis() - startTime);
                    result.setStepId(stepDef.getId());
                    result.setStepType(stepDef.getType());

                    // Store step output in context for subsequent steps
                    if (result.isSuccess() && result.getOutput() != null) {
                        String outputKey = stepDef.getOutputKey() != null ?
                                stepDef.getOutputKey() : stepDef.getId() + ".output";
                        context.set(outputKey, result.getOutput());
                    }

                    return result;
                })
                .onErrorResume(error -> {
                    logger.error("Step execution error. stepId={}, type={}",
                            stepDef.getId(), stepDef.getType(), error);
                    return Mono.just(StepResult.failure(
                            "Step execution error: " + error.getMessage(),
                            System.currentTimeMillis() - startTime
                    ));
                })
                .flatMapMany(result -> {
                    // If step failed and failure strategy is TERMINATE, stop execution
                    if (!result.isSuccess() && stepDef.getFailureStrategy() == FailureStrategy.TERMINATE) {
                        return Flux.just(result);
                    }

                    // Continue with next step
                    return Flux.concat(
                            Flux.just(result),
                            executeStepsSequentially(steps, context, startIndex + 1)
                    );
                });
    }
}

