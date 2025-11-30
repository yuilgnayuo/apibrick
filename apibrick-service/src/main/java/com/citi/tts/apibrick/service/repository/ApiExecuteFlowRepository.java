package com.citi.tts.apibrick.service.repository;

import com.citi.tts.apibrick.service.domain.ApiExecuteFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA persistence interface for ApiExecuteFlow
 */
@Repository
public interface ApiExecuteFlowRepository extends JpaRepository<ApiExecuteFlow, Long> {
    /**
     * Query by tenant ID + flow identifier
     */
    Optional<ApiExecuteFlow> findByTenantIdAndFlowCode(String tenantId, String flowCode);
}