package com.citi.tts.apibrick.core.datasource.mongo;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.QueryParser;
import org.bson.Document;
import java.util.List;
import java.util.Map;

/**
 * MongoDB Query Parser
 * Converts visual query configuration to MongoDB Bson Document
 */
public class MongoQueryParser implements QueryParser {
    
    @Override
    public Object parse(Map<String, Object> queryConfig) {
        // Extract query conditions from configuration
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) queryConfig.get("conditions");
        
        Document queryFilter = new Document();
        
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                String field = (String) condition.get("field");
                String operator = (String) condition.get("operator");
                Object value = condition.get("value");
                
                if (field == null || operator == null) {
                    continue;
                }
                
                // Convert operator to MongoDB query operator
                switch (operator.toLowerCase()) {
                    case "eq":
                    case "equals":
                        queryFilter.append(field, value);
                        break;
                    case "ne":
                    case "not_equals":
                        queryFilter.append(field, new Document("$ne", value));
                        break;
                    case "gt":
                    case "greater_than":
                        queryFilter.append(field, new Document("$gt", value));
                        break;
                    case "gte":
                    case "greater_than_or_equal":
                        queryFilter.append(field, new Document("$gte", value));
                        break;
                    case "lt":
                    case "less_than":
                        queryFilter.append(field, new Document("$lt", value));
                        break;
                    case "lte":
                    case "less_than_or_equal":
                        queryFilter.append(field, new Document("$lte", value));
                        break;
                    case "in":
                        queryFilter.append(field, new Document("$in", value));
                        break;
                    case "nin":
                    case "not_in":
                        queryFilter.append(field, new Document("$nin", value));
                        break;
                    case "like":
                    case "contains":
                        queryFilter.append(field, new Document("$regex", value).append("$options", "i"));
                        break;
                    default:
                        // Default to equality
                        queryFilter.append(field, value);
                }
            }
        }
        
        return queryFilter;
    }
    
    @Override
    public DataSourceType supportType() {
        return DataSourceType.MONGO;
    }
    
    @Override
    public void validate(Map<String, Object> queryConfig) {
        QueryParser.super.validate(queryConfig);
        // Additional MongoDB-specific validation can be added here
    }
}

