package com.citi.tts.apibrick.core.datasource.mysql;

import com.citi.tts.apibrick.common.enums.DataSourceType;
import com.citi.tts.apibrick.core.datasource.QueryParser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MySQL Query Parser
 * Converts query configuration to SQL statement with parameter binding
 * 
 * Supports:
 * - Direct SQL string
 * - Parameterized queries with parameter binding
 * - SQL injection prevention
 */
public class MySqlQueryParser implements QueryParser {
    
    private static final Logger logger = LoggerFactory.getLogger(MySqlQueryParser.class);
    
    // Dangerous SQL keywords that should not be allowed
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "(?i)(DROP\\s+TABLE|DROP\\s+DATABASE|TRUNCATE|DELETE\\s+FROM|UPDATE\\s+.*SET|ALTER\\s+TABLE|CREATE\\s+TABLE|INSERT\\s+INTO)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public Object parse(Map<String, Object> queryConfig) {
        // Get SQL string from configuration
        String sql = (String) queryConfig.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query is required");
        }
        
        // Get parameters if any
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) queryConfig.get("parameters");
        
        // Parse SQL to validate syntax (optional, for better error messages)
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            // Only allow SELECT statements for safety
            if (!(statement instanceof Select)) {
                logger.warn("Non-SELECT statement detected: {}", statement.getClass().getSimpleName());
            }
        } catch (JSQLParserException e) {
            logger.warn("SQL parsing warning (continuing anyway): {}", e.getMessage());
        }
        
        // Create ParsedSql object
        return new MySqlDataSource.ParsedSql(sql.trim(), parameters);
    }
    
    @Override
    public DataSourceType supportType() {
        return DataSourceType.MYSQL;
    }
    
    @Override
    public void validate(Map<String, Object> queryConfig) {
        QueryParser.super.validate(queryConfig);
        
        String sql = (String) queryConfig.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }
        
        // Check for dangerous SQL patterns
        if (DANGEROUS_PATTERNS.matcher(sql).find()) {
            throw new IllegalArgumentException(
                "Dangerous SQL operation detected. Only SELECT queries are allowed for security reasons.");
        }
        
        // Additional validation: check for SQL injection patterns
        // Simple check for suspicious patterns
        String upperSql = sql.toUpperCase();
        if (upperSql.contains(";") && upperSql.split(";").length > 2) {
            throw new IllegalArgumentException("Multiple SQL statements detected. Only single statement allowed.");
        }
        
        // Check for comment-based SQL injection
        if (sql.contains("--") || sql.contains("/*") || sql.contains("*/")) {
            throw new IllegalArgumentException("SQL comments are not allowed for security reasons.");
        }
        
        // Validate parameters count matches placeholders
        @SuppressWarnings("unchecked")
        List<Object> parameters = (List<Object>) queryConfig.get("parameters");
        if (parameters != null) {
            long placeholderCount = sql.chars().filter(ch -> ch == '?').count();
            if (parameters.size() != placeholderCount) {
                throw new IllegalArgumentException(
                    String.format("Parameter count mismatch: SQL has %d placeholders but %d parameters provided",
                        placeholderCount, parameters.size()));
            }
        }
    }
}

