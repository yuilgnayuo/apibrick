package com.citi.tts.apibrick.core.datasource.mysql;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.DataSourceFactory;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * MySQL Data Source Factory
 * Creates MySQL data source instances using R2DBC
 */
public class MySqlDataSourceFactory implements DataSourceFactory {
    
    @Override
    public Mono<DataSource> create(Map<String, Object> config, String tenantId, String env) {
        MySqlDataSource dataSource = new MySqlDataSource();
        return dataSource.init(config, tenantId, env)
            .then(Mono.just(dataSource));
    }
    
    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.MYSQL;
    }
}

