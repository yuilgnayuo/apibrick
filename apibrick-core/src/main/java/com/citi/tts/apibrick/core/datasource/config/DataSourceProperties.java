package com.citi.tts.apibrick.core.datasource.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "database.mysql")
public class DataSourceProperties {

    private String host;
    private String port;
    private String dbName;
    private String username;
    private String password;

    public Map<String, Object> toMap() {
        Map<String, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("host", this.host);
        dataSourceMap.put("port", this.port);
        dataSourceMap.put("dbName", this.dbName);
        dataSourceMap.put("username", this.username);
        dataSourceMap.put("password", this.password);
        return dataSourceMap;
    }
}