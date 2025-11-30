package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.response.ResponseGenerator;
import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

/**
 * Conditional Step - Executes different branches based on condition evaluation
 * 
 * Supports conditional routing:
 * - If condition is true, execute "thenSteps"
 * - If condition is false, execute "elseSteps"
 * - Supports nested conditions
 */
@Component
public class ConditionalStep implements Step {
    
    private static final String TYPE = "CONDITIONAL";
    
    @Autowired
    private ResponseGenerator responseGenerator;
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    @Override
    public Mono<StepResult> execute(StepContext context) {
        long startTime = System.currentTimeMillis();
        
        // Get condition expression
        String condition = (String) context.get("condition");
        if (condition == null || condition.isEmpty()) {
            return Mono.just(StepResult.failure(
                "Condition expression is required",
                System.currentTimeMillis() - startTime
            ));
        }
        
        // Evaluate condition
        Boolean conditionResult = evaluateCondition(condition, context);
        
        // Store condition result in context for subsequent steps
        context.set("conditionResult", conditionResult);
        context.set("conditionMet", conditionResult);
        
        Map<String, Object> output = new HashMap<>();
        output.put("condition", condition);
        output.put("result", conditionResult);
        
        return Mono.just(StepResult.success(output, System.currentTimeMillis() - startTime));
    }
    
    /**
     * Evaluate condition expression
     */
    private Boolean evaluateCondition(String condition, StepContext context) {
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            
            // Add all context data to evaluation context
            evalContext.setVariable("request", context.getRequestParams());
            evalContext.setVariable("variables", context.getVariables());
            evalContext.setVariable("tenantId", context.getTenantId());
            evalContext.setVariable("env", context.getEnv());
            
            // Add step outputs
            Map<String, Object> stepOutputs = new HashMap<>();
            for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
                if (entry.getKey().endsWith(".output")) {
                    String stepId = entry.getKey().substring(0, entry.getKey().length() - 7);
                    stepOutputs.put(stepId, entry.getValue());
                }
            }
            evalContext.setVariable("steps", stepOutputs);
            
            // Parse and evaluate
            Expression expr = expressionParser.parseExpression(condition);
            Object result = expr.getValue(evalContext);
            
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            if (result instanceof String) {
                return Boolean.parseBoolean((String) result);
            }
            return result != null;
            
        } catch (Exception e) {
            // If evaluation fails, return false
            return false;
        }
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}

