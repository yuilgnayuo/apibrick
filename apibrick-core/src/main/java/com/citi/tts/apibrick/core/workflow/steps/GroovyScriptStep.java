package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.script.GroovyScriptEngine;
import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

/**
 * Groovy Script Step - Executes Groovy scripts in sandbox
 * 
 * This step allows dynamic logic injection into workflows
 * Scripts can access context variables and return results
 */
@Component
public class GroovyScriptStep implements Step {
    
    private static final String TYPE = "GROOVY_SCRIPT";
    
    @Autowired
    private GroovyScriptEngine scriptEngine;
    
    @Override
    public Mono<StepResult> execute(StepContext context) {
        long startTime = System.currentTimeMillis();
        
        // Get script code from step configuration
        String scriptCode = (String) context.get("scriptCode");
        if (scriptCode == null || scriptCode.isEmpty()) {
            return Mono.just(StepResult.failure(
                "Script code is required", 
                System.currentTimeMillis() - startTime
            ));
        }
        
        // Build script context from workflow context
        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.putAll(context.getRequestParams());
        scriptContext.putAll(context.getVariables());
        scriptContext.put("tenantId", context.getTenantId());
        scriptContext.put("env", context.getEnv());
        
        // Execute script
        return scriptEngine.execute(scriptCode, scriptContext)
            .map(result -> {
                Map<String, Object> output = new HashMap<>();
                output.put("result", result);
                return StepResult.success(output, System.currentTimeMillis() - startTime);
            })
            .onErrorResume(error -> {
                return Mono.just(StepResult.failure(
                    "Script execution error: " + error.getMessage(),
                    System.currentTimeMillis() - startTime
                ));
            });
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}

