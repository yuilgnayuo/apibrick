package com.citi.tts.apibrick.service.domain;

import com.citi.tts.apibrick.core.workflow.engine.Step;
import com.citi.tts.apibrick.service.tool.JsonStringConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API orchestration execution flow model (persistent model after orchestration JSON parsing)
 */
@Data
@Entity
@Table(name = "api_execute_flow", indexes = {
        @Index(name = "idx_tenant_flow_code", columnList = "tenant_id, flow_code"),
        @Index(name = "idx_flow_name", columnList = "flow_name")
})
@DynamicInsert
@DynamicUpdate
public class ApiExecuteFlow {
    /**
     * Primary key ID (auto-increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant ID (multi-tenancy isolation)
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Unique flow identifier (custom-generated, e.g., flow_user_query_001)
     */
    @Column(name = "flow_code", nullable = false, unique = true)
    private String flowCode;

    /**
     * Flow name
     */
    @Column(name = "flow_name", nullable = false)
    private String flowName;

    /**
     * Orchestration step list (JSON format, storing ApiExecuteStep list)
     */
    @Column(name = "step_list", columnDefinition = "TEXT")
    @Convert(converter = JsonStringConverter.class)
    private List<Step> stepList;

    /**
     * Flow description
     */
    @Column(name = "flow_desc")
    private String flowDesc;

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

    // --- Non-persistent fields (temporary storage) ---
    /**
     * Original orchestration JSON (temporary storage for reverse query)
     */
    @Transient
    private String rawFlowJson;
}