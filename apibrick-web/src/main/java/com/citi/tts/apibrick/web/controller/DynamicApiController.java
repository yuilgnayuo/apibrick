package com.citi.tts.apibrick.web.controller;

import com.citi.tts.apibrick.service.api.ApiExecutionService;
import com.citi.tts.apibrick.service.api.ApiMatcherService;
import com.citi.tts.apibrick.service.api.RequestParamResolver;
import com.citi.tts.apibrick.service.api.ResponseFormatter;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Dynamic API Controller
 * 
 * Unified entry point for all dynamic API requests.
 * Handles the complete request processing flow:
 * 1. Extract tenant ID and environment from Reactive Context
 * 2. Match API definition
 * 3. Resolve request parameters
 * 4. Execute workflow
 * 5. Format and return response
 */
@RestController
public class DynamicApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicApiController.class);
    
    @Autowired
    private ApiMatcherService apiMatcherService;
    
    @Autowired
    private RequestParamResolver requestParamResolver;
    
    @Autowired
    private ApiExecutionService apiExecutionService;
    
    @Autowired
    private ResponseFormatter responseFormatter;
    
    /**
     * Handle all dynamic API requests
     * 
     * This method catches all requests that don't match static routes
     * The actual path is extracted from the request
     * 
     * @param exchange ServerWebExchange containing request information
     * @return Mono<ResponseEntity<Object>> HTTP response
     */
    @RequestMapping(value = "/**", method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                                            org.springframework.web.bind.annotation.RequestMethod.POST,
                                            org.springframework.web.bind.annotation.RequestMethod.PUT,
                                            org.springframework.web.bind.annotation.RequestMethod.DELETE,
                                            org.springframework.web.bind.annotation.RequestMethod.PATCH})
    public Mono<ResponseEntity<Object>> handleRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        String httpMethod = exchange.getRequest().getMethod().name();
        requestPath = requestPath.substring(0,requestPath.lastIndexOf('/'));
        logger.info("Received dynamic API request: path={}, method={}", requestPath, httpMethod);
        
        // Extract tenant ID and environment from Reactive Context
        String finalRequestPath = requestPath;
        return Mono.deferContextual(ctx -> {
            String tenantId = ctx.getOrDefault("tenantId", "tenant_001");
            String env = ctx.getOrDefault("env", "DEV");
            
            logger.debug("Request context: tenantId={}, env={}", tenantId, env);
            
            // Step 1: Match API
            return apiMatcherService.matchApi(tenantId, finalRequestPath, httpMethod)
                    .switchIfEmpty(Mono.fromCallable(() -> {
                        logger.warn("API not found: path={}, method={}, tenantId={}",
                                finalRequestPath, httpMethod, tenantId);
//                        return ;
                        return new ApiDefinition(); // Dummy to trigger error handling
                    }))
                    .flatMap(apiDef -> {
//                        if(StringUtils.isEmpty(apiDef.getApiCode())) {
//                            return ResponseEntity.ok(responseFormatter.formatNotFoundResponse().getBody());
//                        }
                        logger.debug("Matched API: apiCode={}", apiDef.getApiCode());
                        
                        // Step 2: Resolve request parameters
                        return requestParamResolver.resolveParams(exchange, apiDef)
                                .flatMap(requestParams -> {
                                    logger.debug("Resolved parameters: {}", requestParams.keySet());
                                    
                                    // Step 3: Execute workflow
                                    return apiExecutionService.executeApi(apiDef, requestParams, tenantId, env)
                                            .flatMap(workflowResult -> {
                                                logger.debug("Workflow execution completed: success={}", 
                                                           workflowResult.isSuccess());
                                                
                                                // Step 4: Format response
                                                ResponseEntity<Object> response = 
                                                        responseFormatter.formatResponse(workflowResult, apiDef);
                                                
                                                return Mono.just(response);
                                            })
                                            .onErrorResume(error -> {
                                                logger.error("Workflow execution error: apiCode={}", 
                                                           apiDef.getApiCode(), error);
                                                
                                                // Format error response
                                                ResponseEntity<Object> errorResponse = 
                                                        responseFormatter.formatErrorResponse(error, apiDef);
                                                
                                                return Mono.just(errorResponse);
                                            });
                                })
                                .onErrorResume(error -> {
                                    logger.error("Parameter resolution error: apiCode={}", 
                                               apiDef.getApiCode(), error);
                                    
                                    // Format validation error response
                                    String errorMsg = error.getMessage() != null ? 
                                            error.getMessage() : "Parameter validation failed";
                                    ResponseEntity<Object> errorResponse = 
                                            responseFormatter.formatValidationErrorResponse(errorMsg);
                                    
                                    return Mono.just(errorResponse);
                                });
                    })
                    .onErrorResume(error -> {
                        logger.error("API matching error: path={}, method={}",
                                finalRequestPath, httpMethod, error);
                        
                        // Return generic error response
                        ResponseEntity<Object> errorResponse = 
                                responseFormatter.formatNotFoundResponse();
                        
                        return Mono.just(errorResponse);
                    });
        });
    }
}

