package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.response.ResponseGenerator;
import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

/**
 * Response Step - Generates final API response based on response template
 * 
 * This step should be the last step in a workflow
 * It uses ResponseGenerator to dynamically generate the response based on:
 * - Request parameters
 * - Step execution results
 * - Response template configuration
 */
@Component
public class ResponseStep implements Step {
    
    private static final String TYPE = "RESPONSE";
    
    @Autowired
    private ResponseGenerator responseGenerator;
    
    @Override
    public Mono<StepResult> execute(StepContext context) {
        long startTime = System.currentTimeMillis();
        
        // Get response template from step configuration
        Map<String, Object> responseTemplate = context.get("responseTemplate");
        
        // Generate response
        Map<String, Object> response = responseGenerator.generate(responseTemplate, context);
        
        // Store response in context
        context.set("finalResponse", response);
        
        Map<String, Object> output = new HashMap<>();
        output.put("response", response);
        
        return Mono.just(StepResult.success(output, System.currentTimeMillis() - startTime));
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}

