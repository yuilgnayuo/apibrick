package com.citi.tts.apibrick.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Connection Test
 * 
 * Tests MySQL database connection to the "apibrick" database
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
public class DatabaseConnectionTest {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void testDataSourceExists() {
        assertNotNull(dataSource, "DataSource should be configured");
    }
    
    @Test
    void testDatabaseConnection() throws Exception {
        assertNotNull(dataSource, "DataSource should be configured");
        
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
            
            System.out.println("Database Product: " + productName);
            System.out.println("Database Version: " + productVersion);
            System.out.println("Database Name: " + databaseName);
            System.out.println("Connection URL: " + metaData.getURL());
            System.out.println("Driver Name: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            
            assertTrue(productName.toLowerCase().contains("mysql"), 
                      "Database should be MySQL");
        }
    }
    
    @Test
    void testJdbcTemplateQuery() {
        if (jdbcTemplate == null) {
            System.out.println("JdbcTemplate is not available, skipping test");
            return;
        }
        
        // Test simple query
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertNotNull(result, "Query result should not be null");
        assertEquals(1, result, "Query result should be 1");
        
        // Test database name query
        String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertEquals("apibrick", dbName, "Database name should be 'apibrick'");
        
        System.out.println("JdbcTemplate query test passed. Database: " + dbName);
    }
    
    @Test
    void testListTables() throws Exception {
        assertNotNull(dataSource, "DataSource should be configured");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // List all tables in the database
            ResultSet tables = metaData.getTables("apibrick", null, "%", new String[]{"TABLE"});
            
            System.out.println("\n=== Tables in database 'apibrick' ===");
            int tableCount = 0;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                System.out.println("Table: " + tableName + " (Type: " + tableType + ")");
                tableCount++;
            }
            System.out.println("Total tables: " + tableCount);
            
            assertTrue(tableCount >= 0, "Should be able to list tables");
        }
    }
}

