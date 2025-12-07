package com.citi.tts.apibrick.core.datasource.mysql;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.DataConverter;
import com.citi.tts.apibrick.core.datasource.QueryParser;
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.asyncer.r2dbc.mysql.constant.SslMode;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * MySQL Data Source Implementation
 * <p>
 * Uses R2DBC (Reactive Relational Database Connectivity) for non-blocking operations
 * Supports connection pooling for better performance
 * <p>
 * Map<String, Object> config = Map.of(
 * "host", "localhost",
 * "port", 3306,
 * "database", "apibrick",
 * "username", "root",
 * "password", "123456"
 * );
 * <p>
 * dataSourceManager.getOrCreateDataSource(
 * "mysql-ds-1",
 * DataSourceType.MYSQL,
 * config,
 * "tenant_001",
 * "DEV"
 * );
 */
public class MySqlDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(MySqlDataSource.class);

    private ConnectionPool connectionPool;
    private String tenantId;
    private final DataSourceType type = DataSourceType.MYSQL;

    private final QueryParser queryParser = new MySqlQueryParser();
    private final DataConverter dataConverter = new MySqlDataConverter();

    @Override
    public Mono<Void> init(Map<String, Object> config, String tenantId, String env) {
        this.tenantId = tenantId;

        try {
            // Extract connection configuration
            String host = (String) config.get("host");
            String portStr = String.valueOf(config.getOrDefault("port", 3306));
            int port = Integer.parseInt(portStr);
            String database = (String) config.get("dbName");
            String username = (String) config.get("username");
            String password = (String) config.get("password");

            if (host == null || database == null || username == null || password == null) {
                return Mono.error(new IllegalArgumentException(
                        "MySQL connection requires: host, database, username, password"));
            }

            // Build MySQL connection configuration
            MySqlConnectionConfiguration connectionConfig = MySqlConnectionConfiguration.builder()
                    .host(host)
                    .port(port)
                    .database(database)
                    .username(username)
                    .password(password)
                    .connectTimeout(Duration.ofSeconds(10))
                    .sslMode(SslMode.DISABLED)
                    .build();
            // Create connection factory
            ConnectionFactory connectionFactory = MySqlConnectionFactory.from(connectionConfig);

            // Create connection pool configuration
            ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxIdleTime(Duration.ofMinutes(5))
                    .initialSize(1)
                    .maxSize(5)
                    .maxCreateConnectionTime(Duration.ofSeconds(10))
                    .maxAcquireTime(Duration.ofSeconds(8))
                    .maxLifeTime(Duration.ofHours(1))
                    .validationQuery("SELECT 1")
                    .acquireRetry(1)
                    .build();

            // Create connection pool
            this.connectionPool = new ConnectionPool(poolConfig);

            logger.info("MySQL data source initialized. tenantId={}, host={}, database={}",
                    tenantId, host, database);

            return Mono.empty();
        } catch (Exception e) {
            logger.error("Failed to initialize MySQL data source", e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Map<String, Object>> executeQuery(Map<String, Object> queryConfig) {
        if (connectionPool == null) {
            return Mono.error(new IllegalStateException("MySQL data source not initialized"));
        }

        try {
            // Parse query configuration to SQL statement
            Object parsedQuery = queryParser.parse(queryConfig);
            if (!(parsedQuery instanceof ParsedSql parsedSql)) {
                return Mono.error(new IllegalArgumentException("Invalid MySQL query format"));
            }

            // Validate query (prevent SQL injection)
            queryParser.validate(queryConfig);

            // Execute query using connection pool
            return Mono.from(connectionPool.create())
                    .flatMap(connection -> {
                        try {
                            // Create statement
                            Statement statement = connection.createStatement(parsedSql.getSql());

                            // Set parameters if any
                            if (parsedSql.getParameters() != null) {
                                for (int i = 0; i < parsedSql.getParameters().size(); i++) {
                                    statement.bind(i, parsedSql.getParameters().get(i));
                                }
                            }

                            // Execute query and collect results
                            return Flux.from(statement.execute())
                                    .flatMap(result -> {
                                        return Flux.from(result.map((row, metadata) -> {
                                            // Convert row to Map
                                            Map<String, Object> rowMap = new HashMap<>();
                                            for (io.r2dbc.spi.ColumnMetadata column : metadata.getColumnMetadatas()) {
                                                String columnName = column.getName();
                                                Object value = row.get(columnName);
                                                rowMap.put(columnName, value);
                                            }
                                            return rowMap;
                                        }));
                                    })
                                    .collectList()
                                    .map(rows -> {
                                        // Convert results using data converter
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> fieldMapping = (Map<String, Object>) queryConfig.get("fieldMapping");

                                        Map<String, Object> result = new HashMap<>();
                                        if (rows.size() == 1) {
                                            // Single row result
                                            result = dataConverter.convert(rows.get(0), fieldMapping);
                                        } else {
                                            // Multiple rows result
                                            result.put("data", rows.stream()
                                                    .map(row -> dataConverter.convert(row, fieldMapping))
                                                    .toList());
                                            result.put("count", rows.size());
                                        }

                                        return result;
                                    })
                                    .doFinally(signalType -> {
                                        // Release connection back to pool
                                        connection.close();
                                    });
                        } catch (Exception e) {
                            connection.close();
                            return Mono.error(e);
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("MySQL query execution error", error);
                        return Mono.error(error);
                    });

        } catch (Exception e) {
            logger.error("MySQL query error", e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Boolean> testConnection() {
        if (connectionPool == null) {
            return Mono.just(false);
        }
        try {
            return Mono.from(connectionPool.create())
                    .flatMap(connection -> {
                        return Mono.from(connection.createStatement("SELECT 1").execute())
                                .flatMap(this::processResult)
                                .doFinally(signalType -> connection.close());
                    })
                    .timeout(Duration.ofSeconds(5))
                    .onErrorReturn(false);
        } catch (Exception e) {
            return Mono.just(false);
        }
    }

    private Mono<Boolean> processResult(Result result) {
        return Mono.from(result.map((row, metadata) -> row.get(0)))
                .doFinally(signalType -> {
                    if (result instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) result).close();
                        } catch (Exception e) {
                            logger.error("Failed to close ResultSet", e);
                        }
                    }
                })
                .map(ignored -> true);
    }

    @Override
    public void close() {
        if (connectionPool != null) {
            connectionPool.dispose();
            logger.info("MySQL data source closed. tenantId={}", tenantId);
        }
    }

    @Override
    public DataSourceType getType() {
        return type;
    }

    /**
     * Internal class to hold parsed SQL and parameters
     */
    static class ParsedSql {
        private final String sql;
        private final List<Object> parameters;

        public ParsedSql(String sql, List<Object> parameters) {
            this.sql = sql;
            this.parameters = parameters != null ? parameters : new ArrayList<>();
        }

        public String getSql() {
            return sql;
        }

        public List<Object> getParameters() {
            return parameters;
        }
    }
}

