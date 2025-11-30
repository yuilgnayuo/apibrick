package com.citi.tts.apibrick.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Service - Collects metrics and logs for observability
 * 
 * Features:
 * - Metrics collection (call count, success rate, latency)
 * - Log aggregation
 * - Alert triggering
 * - Performance analysis
 */
@Service
public class MonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter apiCallCounter;
    private final Counter apiSuccessCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;
    
    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.apiCallCounter = Counter.builder("apibrick.api.calls")
            .description("Total number of API calls")
            .register(meterRegistry);
        
        this.apiSuccessCounter = Counter.builder("apibrick.api.success")
            .description("Number of successful API calls")
            .register(meterRegistry);
        
        this.apiErrorCounter = Counter.builder("apibrick.api.errors")
            .description("Number of failed API calls")
            .register(meterRegistry);
        
        this.apiLatencyTimer = Timer.builder("apibrick.api.latency")
            .description("API call latency")
            .register(meterRegistry);
    }
    
    /**
     * Record API call metrics
     * 
     * @param apiId API ID
     * @param tenantId Tenant ID
     * @param success Whether call was successful
     * @param latencyMs Latency in milliseconds
     */
    public void recordApiCall(String apiId, String tenantId, boolean success, long latencyMs) {
        apiCallCounter.increment();
        
        if (success) {
            apiSuccessCounter.increment();
        } else {
            apiErrorCounter.increment();
        }
        
        apiLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // Log the call
        logger.debug("API call recorded. apiId={}, tenantId={}, success={}, latency={}ms",
                    apiId, tenantId, success, latencyMs);
    }
    
    /**
     * Record workflow execution metrics
     * 
     * @param flowId Flow ID
     * @param tenantId Tenant ID
     * @param success Whether execution was successful
     * @param executeTime Execution time in milliseconds
     */
    public void recordWorkflowExecution(String flowId, String tenantId, boolean success, long executeTime) {
        Counter.builder("apibrick.workflow.executions")
            .tag("flowId", flowId)
            .tag("tenantId", tenantId)
            .tag("status", success ? "success" : "failure")
            .register(meterRegistry)
            .increment();
        
        Timer.builder("apibrick.workflow.executeTime")
            .tag("flowId", flowId)
            .tag("tenantId", tenantId)
            .register(meterRegistry)
            .record(executeTime, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Get API metrics summary
     * 
     * @param apiId API ID (optional, null for all APIs)
     * @param tenantId Tenant ID (optional, null for all tenants)
     * @return Mono<Map<String, Object>> Metrics summary
     */
    public Mono<Map<String, Object>> getMetricsSummary(String apiId, String tenantId) {
        return Mono.fromCallable(() -> {
            Map<String, Object> summary = new HashMap<>();
            
            // Get metrics from registry
            double totalCalls = apiCallCounter.count();
            double successCalls = apiSuccessCounter.count();
            double errorCalls = apiErrorCounter.count();
            
            summary.put("totalCalls", totalCalls);
            summary.put("successCalls", successCalls);
            summary.put("errorCalls", errorCalls);
            summary.put("successRate", totalCalls > 0 ? (successCalls / totalCalls) * 100 : 0);
            
            // Get latency statistics
            double p50 = apiLatencyTimer.percentile(0.5, java.util.concurrent.TimeUnit.MILLISECONDS);
            double p95 = apiLatencyTimer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS);
            double p99 = apiLatencyTimer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            summary.put("latencyP50", p50);
            summary.put("latencyP95", p95);
            summary.put("latencyP99", p99);
            
            return summary;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Check if alert conditions are met
     * 
     * @param apiId API ID
     * @param tenantId Tenant ID
     * @return Mono<Boolean> true if alert should be triggered
     */
    public Mono<Boolean> checkAlerts(String apiId, String tenantId) {
        return getMetricsSummary(apiId, tenantId)
            .map(summary -> {
                double successRate = (Double) summary.get("successRate");
                double p99Latency = (Double) summary.get("latencyP99");
                
                // Alert if success rate < 99% or P99 latency > 1000ms
                boolean shouldAlert = successRate < 99.0 || p99Latency > 1000.0;
                
                if (shouldAlert) {
                    logger.warn("Alert triggered. apiId={}, tenantId={}, successRate={}%, p99Latency={}ms",
                               apiId, tenantId, successRate, p99Latency);
                }
                
                return shouldAlert;
            });
    }
}

