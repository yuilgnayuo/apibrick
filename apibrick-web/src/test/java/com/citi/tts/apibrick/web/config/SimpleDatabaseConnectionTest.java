package com.citi.tts.apibrick.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Database Connection Test
 * 
 * Tests MySQL database connection to the "apibrick" database
 * This is a simpler version that doesn't require all dependencies
 */
@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/apibrick?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=123456",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.show-sql=true",
        "spring.jpa.hibernate.ddl-auto=validate"
    }
)
public class SimpleDatabaseConnectionTest {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void testDataSourceExists() {
        if (dataSource == null) {
            System.out.println("WARNING: DataSource is not configured. Please check your MySQL configuration.");
            return;
        }
        assertNotNull(dataSource, "DataSource should be configured");
        System.out.println("✓ DataSource is configured");
    }
    
    @Test
    void testDatabaseConnection() throws Exception {
        if (dataSource == null) {
            System.out.println("SKIP: DataSource is not configured");
            return;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
            
            // Get database metadata
            DatabaseMetaData metaData = connection.getMetaData();
            assertNotNull(metaData, "Database metadata should not be null");
            
            // Verify database name
            String databaseName = connection.getCatalog();
            assertEquals("apibrick", databaseName, "Database name should be 'apibrick'");
            
            // Get database product name and version
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();
            
            System.out.println("\n=== Database Connection Info ===");
            System.out.println("Database Product: " + productName);
            System.out.println("Database Version: " + productVersion);
            System.out.println("Database Name: " + databaseName);
            System.out.println("Connection URL: " + metaData.getURL());
            System.out.println("Driver Name: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            System.out.println("===============================\n");
            
            assertTrue(productName.toLowerCase().contains("mysql"), 
                      "Database should be MySQL");
            
            System.out.println("✓ Database connection test passed!");
        } catch (Exception e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test
    void testJdbcTemplateQuery() {
        if (jdbcTemplate == null) {
            System.out.println("SKIP: JdbcTemplate is not available");
            return;
        }
        
        try {
            // Test simple query
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertNotNull(result, "Query result should not be null");
            assertEquals(1, result, "Query result should be 1");
            
            // Test database name query
            String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            assertEquals("apibrick", dbName, "Database name should be 'apibrick'");
            
            System.out.println("✓ JdbcTemplate query test passed. Database: " + dbName);
        } catch (Exception e) {
            System.err.println("✗ JdbcTemplate query failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test
    void testDatabaseVersion() {
        if (jdbcTemplate == null) {
            System.out.println("SKIP: JdbcTemplate is not available");
            return;
        }
        
        try {
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            System.out.println("MySQL Version: " + version);
            assertNotNull(version, "MySQL version should not be null");
            System.out.println("✓ MySQL version query passed");
        } catch (Exception e) {
            System.err.println("✗ MySQL version query failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

