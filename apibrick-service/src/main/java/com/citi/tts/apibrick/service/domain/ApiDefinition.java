package com.citi.tts.apibrick.service.domain;

import com.citi.tts.apibrick.common.enums.ApiStatus;
import com.citi.tts.apibrick.common.enums.EncryptAlgorithm;
import com.citi.tts.apibrick.service.tool.JsonStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Custom API definition model after Swagger parsing (core persistent entity)
 */
@Data
@Entity
@Table(name = "api_definition", indexes = {
        @Index(name = "idx_tenant_api_path", columnList = "tenant_id, api_path"),
        @Index(name = "idx_api_status", columnList = "api_status")
})
@DynamicInsert
@DynamicUpdate
public class ApiDefinition {
    /**
     * Primary key ID (auto-increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant ID (core field for multi-tenancy isolation)
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Unique API identifier (operationId in Swagger, or custom-generated)
     */
    @Column(name = "api_code", nullable = false, unique = true)
    private String apiCode;

    /**
     * API name (summary/title parsed from Swagger)
     */
    @Column(name = "api_name", nullable = false)
    private String apiName;

    /**
     * API request path (path parsed from Swagger, e.g., /api/v1/user/{userId})
     */
    @Column(name = "api_path", nullable = false)
    private String apiPath;

    /**
     * HTTP request method (method parsed from Swagger, e.g., GET/POST/PUT/DELETE)
     */
    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    /**
     * API status (Enabled/Disabled/Draft)
     */
    @Column(name = "api_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApiStatus apiStatus;

    /**
     * Encryption algorithm (AES/RSA/NONE)
     */
    @Column(name = "encrypt_algorithm")
    @Enumerated(EnumType.STRING)
    private EncryptAlgorithm encryptAlgorithm = EncryptAlgorithm.NONE;

    /**
     * Associated data source ID list (multiple separated by commas, e.g., ds_mysql_001,ds_redis_001)
     */
    @Column(name = "data_source_ids")
    private String dataSourceIds;

    /**
     * Request parameter definition parsed from Swagger (JSON format, storing parameter name, type, required status, etc.)
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    @Convert(converter = JsonStringConverter.class)
    private List<Map<String, Object>> requestParams;

    /**
     * Response definition parsed from Swagger (JSON format, storing response code, response body structure, etc.)
     */
    @Column(name = "response_def", columnDefinition = "TEXT")
    @Convert(converter = JsonStringConverter.class)
    private Map<Integer, Map<String, Object>> responseDef;

    /**
     * Associated orchestration flow ID (associates with flowCode of ApiExecuteFlow)
     */
    @Column(name = "flow_code")
    private String flowCode;

    /**
     * Creation time
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * Update time
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    // --- Non-persistent fields (temporary storage, not stored in the database) ---
    /**
     * Original Swagger metadata (temporary storage for reverse query)
     */
    @JsonIgnore
    @Transient
    private String swaggerRawMeta;
}