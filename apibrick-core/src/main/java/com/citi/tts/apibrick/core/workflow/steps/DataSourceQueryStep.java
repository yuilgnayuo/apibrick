package com.citi.tts.apibrick.core.workflow.steps;

import com.citi.tts.apibrick.core.datasource.manager.DataSourceManager;
import com.citi.tts.apibrick.core.datasource.DataSourceType;
import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.core.workflow.engine.StepContext;
import com.citi.tts.apibrick.core.workflow.engine.StepResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Source Query Step - Executes queries against configured data sources
 * 
 * Supports all data source types (Oracle, MySQL, PostgreSQL, MongoDB)
 * Query configuration is data source specific
 */
@Component
public class DataSourceQueryStep implements Step {
    
    private static final String TYPE = "DATASOURCE_QUERY";
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    @Override
    public Mono<StepResult> execute(StepContext context) {
        long startTime = System.currentTimeMillis();
        
        // Get step configuration from context
        String datasourceId = (String) context.get("datasourceId");
        String datasourceTypeStr = (String) context.get("datasourceType");
        
        if (datasourceId == null || datasourceId.isEmpty()) {
            return Mono.just(StepResult.failure(
                "Data source ID is required",
                System.currentTimeMillis() - startTime
            ));
        }
        
        if (datasourceTypeStr == null || datasourceTypeStr.isEmpty()) {
            return Mono.just(StepResult.failure(
                "Data source type is required",
                System.currentTimeMillis() - startTime
            ));
        }
        
        // Parse data source type
        DataSourceType datasourceType;
        try {
            datasourceType = DataSourceType.valueOf(datasourceTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.just(StepResult.failure(
                "Invalid data source type: " + datasourceTypeStr,
                System.currentTimeMillis() - startTime
            ));
        }
        
        // Get query configuration
        Map<String, Object> queryConfig = context.get("queryConfig");
        if (queryConfig == null) {
            queryConfig = new HashMap<>();
        }
        
        // Get data source (assuming it's already initialized)
        // In real implementation, you would get data source config from database
        Map<String, Object> finalQueryConfig = queryConfig;
        return dataSourceManager.getDataSource(datasourceId, context.getTenantId())
            .switchIfEmpty(Mono.error(new IllegalStateException(
                "Data source not found: " + datasourceId)))
            .flatMap(dataSource -> {
                // Execute query
                return dataSource.executeQuery(finalQueryConfig);
            })
            .map(queryResult -> {
                Map<String, Object> output = new HashMap<>();
                output.put("data", queryResult);
                return StepResult.success(output, System.currentTimeMillis() - startTime);
            })
            .onErrorResume(error -> {
                return Mono.just(StepResult.failure(
                    "Data source query error: " + error.getMessage(),
                    System.currentTimeMillis() - startTime
                ));
            });
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}

