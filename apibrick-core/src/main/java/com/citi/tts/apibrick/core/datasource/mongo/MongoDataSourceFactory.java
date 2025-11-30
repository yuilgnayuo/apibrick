package com.citi.tts.apibrick.core.datasource.mongo;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.DataSourceFactory;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * MongoDB Data Source Factory
 * Creates MongoDB data source instances
 */
public class MongoDataSourceFactory implements DataSourceFactory {
    
    @Override
    public Mono<DataSource> create(Map<String, Object> config, String tenantId, String env) {
        MongoDataSource dataSource = new MongoDataSource();
        return dataSource.init(config, tenantId, env)
            .then(Mono.just(dataSource));
    }
    
    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.MONGO;
    }
}

