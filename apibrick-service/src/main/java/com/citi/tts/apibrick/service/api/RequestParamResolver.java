package com.citi.tts.apibrick.service.api;

import com.citi.tts.apibrick.common.util.JsonUtil;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Request Parameter Resolver
 * 
 * Resolves and validates request parameters from:
 * - Path parameters (e.g., /api/v1/user/{userId})
 * - Query parameters (e.g., ?name=value)
 * - Request body (JSON/form-data)
 * - Request headers
 * 
 * Validates parameters based on Swagger definition in ApiDefinition
 */
@Service
public class RequestParamResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestParamResolver.class);
    
    private final ApiMatcherService apiMatcherService;
    
    public RequestParamResolver(ApiMatcherService apiMatcherService) {
        this.apiMatcherService = apiMatcherService;
    }
    
    /**
     * Resolve all request parameters
     * 
     * @param exchange ServerWebExchange containing request information
     * @param apiDef ApiDefinition for parameter validation
     * @return Mono<Map<String, Object>> containing all resolved parameters
     */
    public Mono<Map<String, Object>> resolveParams(ServerWebExchange exchange, ApiDefinition apiDef) {
        Map<String, Object> params = new HashMap<>();
        
        // Extract path parameters
        String requestPath = exchange.getRequest().getPath().value();
        Map<String, String> pathParams = apiMatcherService.extractPathParams(apiDef.getApiPath(), requestPath);
        params.putAll(pathParams);
        logger.debug("Extracted path parameters: {}", pathParams);
        
        // Extract query parameters
        Map<String, String> queryParams = new HashMap<>();
        exchange.getRequest().getQueryParams().forEach((key, values) -> {
            if (!values.isEmpty()) {
                queryParams.put(key, values.get(0)); // Take first value
            }
        });
        params.putAll(queryParams);
        logger.debug("Extracted query parameters: {}", queryParams);
        
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        exchange.getRequest().getHeaders().forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        });
        params.put("_headers", headers);
        logger.debug("Extracted headers: {}", headers.keySet());
        
        // Extract request body (for POST/PUT/PATCH)
        String method = exchange.getRequest().getMethod().name();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            return extractRequestBody(exchange)
                    .map(body -> {
                        if (body != null && !body.isEmpty()) {
                            params.put("_body", body);
                            // Try to parse as JSON
                            try {
                                Object jsonBody = JsonUtil.parse(body, Object.class);
                                if (jsonBody instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> jsonMap = (Map<String, Object>) jsonBody;
                                    params.putAll(jsonMap);
                                }
                            } catch (Exception e) {
                                logger.debug("Request body is not JSON, storing as string: {}", e.getMessage());
                            }
                        }
                        return params;
                    })
                    .flatMap(p -> validateParams(p, apiDef));
        }
        
        return validateParams(params, apiDef);
    }
    
    /**
     * Extract request body as string
     */
    private Mono<String> extractRequestBody(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .defaultIfEmpty("");
    }
    
    /**
     * Validate parameters against ApiDefinition requestParams
     * 
     * @param params Resolved parameters
     * @param apiDef ApiDefinition containing parameter definitions
     * @return Mono<Map<String, Object>> validated parameters
     */
    private Mono<Map<String, Object>> validateParams(Map<String, Object> params, ApiDefinition apiDef) {
        List<Map<String, Object>> requestParamsDef = apiDef.getRequestParams();
        
        if (requestParamsDef == null || requestParamsDef.isEmpty()) {
            logger.debug("No parameter definitions found, skipping validation");
            return Mono.just(params);
        }
        
        List<String> errors = new ArrayList<>();
        
        for (Map<String, Object> paramDef : requestParamsDef) {
            String name = (String) paramDef.get("paramName");
            String in = (String) paramDef.get("in"); // path, query, header, body
            String dataType = (String) paramDef.get("dataType"); // data type
            Boolean required = (Boolean) paramDef.get("required");
            
            if (required != null && required && !params.containsKey(name)) {
                // Check if it's in the right location
                boolean found = false;
                if ("path".equals(in) && params.containsKey(name)) {
                    found = true;
                } else if ("query".equals(in) && params.containsKey(name)) {
                    found = true;
                } else if ("header".equals(in)) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> headers = (Map<String, String>) params.get("_headers");
                    if (headers != null && headers.containsKey(name)) {
                        found = true;
                    }
                } else if ("body".equals(in) && params.containsKey(name)) {
                    found = true;
                }
                
                if (!found) {
                    errors.add(String.format("Required parameter '%s' (in: %s) is missing", name, in));
                }
            }
        }
        
        if (!errors.isEmpty()) {
            String errorMsg = String.join("; ", errors);
            logger.warn("Parameter validation failed: {}", errorMsg);
            return Mono.error(new IllegalArgumentException("Parameter validation failed: " + errorMsg));
        }
        
        logger.debug("Parameter validation passed");
        return Mono.just(params);
    }
    
    /**
     * Extract path parameters from request path based on API path pattern
     * (Delegates to ApiMatcherService)
     */
    public Map<String, String> extractPathParams(String apiPath, String requestPath) {
        return apiMatcherService.extractPathParams(apiPath, requestPath);
    }
}

