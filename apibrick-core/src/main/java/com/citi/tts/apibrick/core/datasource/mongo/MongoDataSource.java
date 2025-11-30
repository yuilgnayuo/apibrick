package com.citi.tts.apibrick.core.datasource.mongo;

import com.citi.tts.apibrick.core.datasource.DataSource;
import com.citi.tts.apibrick.core.datasource.DataSourceType;
import com.citi.tts.apibrick.core.datasource.DataConverter;
import com.citi.tts.apibrick.core.datasource.QueryParser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * MongoDB Data Source Implementation
 * 
 * Uses ReactiveMongo for non-blocking operations
 * Supports MongoDB Change Streams for data change notifications
 */
public class MongoDataSource implements DataSource {
    
    private static final Logger logger = LoggerFactory.getLogger(MongoDataSource.class);
    
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private String tenantId;
    private DataSourceType type = DataSourceType.MONGO;
    
    private final QueryParser queryParser = new MongoQueryParser();
    private final DataConverter dataConverter = new MongoDataConverter();
    
    @Override
    public Mono<Void> init(Map<String, Object> config, String tenantId, String env) {
        this.tenantId = tenantId;
        
        try {
            // Build connection string
            String host = (String) config.get("host");
            String port = String.valueOf(config.getOrDefault("port", 27017));
            String database = (String) config.get("database");
            String username = (String) config.get("username");
            String password = (String) config.get("password");
            
            String connectionString;
            if (username != null && password != null) {
                connectionString = String.format("mongodb://%s:%s@%s:%s/%s?ssl=true",
                    username, password, host, port, database);
            } else {
                connectionString = String.format("mongodb://%s:%s/%s", host, port, database);
            }
            
            // Create MongoDB client settings
            MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
            
            // Create reactive MongoDB client
            this.mongoClient = MongoClients.create(clientSettings);
            this.mongoDatabase = mongoClient.getDatabase(database);
            
            logger.info("MongoDB data source initialized. tenantId={}, database={}", tenantId, database);
            
            return Mono.empty();
        } catch (Exception e) {
            logger.error("Failed to initialize MongoDB data source", e);
            return Mono.error(e);
        }
    }
    
    @Override
    public Mono<Map<String, Object>> executeQuery(Map<String, Object> queryConfig) {
        try {
            // Parse query configuration to Bson Document
            Object parsedQuery = queryParser.parse(queryConfig);
            if (!(parsedQuery instanceof Document)) {
                return Mono.error(new IllegalArgumentException("Invalid MongoDB query format"));
            }
            
            Document queryFilter = (Document) parsedQuery;
            String collectionName = (String) queryConfig.get("collection");
            
            if (collectionName == null || collectionName.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Collection name is required"));
            }
            
            // Add tenant isolation: prefix collection name with tenant ID
            String isolatedCollectionName = tenantId + "_" + collectionName;
            MongoCollection<Document> collection = mongoDatabase.getCollection(isolatedCollectionName);
            
            // Execute query reactively
            Publisher<Document> publisher = collection.find(queryFilter).first();
            
            return Mono.from(publisher)
                .map(document -> {
                    // Convert MongoDB Document to Map
                    Map<String, Object> fieldMapping = (Map<String, Object>) queryConfig.get("fieldMapping");
                    return dataConverter.convert(document, fieldMapping);
                })
                .onErrorResume(error -> {
                    logger.error("MongoDB query execution error", error);
                    return Mono.error(error);
                });
            
        } catch (Exception e) {
            logger.error("MongoDB query error", e);
            return Mono.error(e);
        }
    }
    
    @Override
    public Mono<Boolean> testConnection() {
        try {
            // Try to execute a simple command
            return Mono.from(mongoDatabase.runCommand(new Document("ping", 1)))
                .map(result -> true)
                .onErrorReturn(false)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorReturn(false);
        } catch (Exception e) {
            return Mono.just(false);
        }
    }
    
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB data source closed. tenantId={}", tenantId);
        }
    }
    
    @Override
    public DataSourceType getType() {
        return type;
    }
}

