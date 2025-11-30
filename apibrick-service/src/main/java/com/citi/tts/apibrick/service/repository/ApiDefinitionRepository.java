package com.citi.tts.apibrick.service.repository;

import com.citi.tts.apibrick.service.domain.ApiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA persistence interface for ApiDefinition
 */
@Repository
public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition, Long> {
    /**
     * Query by tenant ID + API path (multi-tenancy isolation)
     */
    Optional<ApiDefinition> findByTenantIdAndApiPath(String tenantId, String apiPath);

    /**
     * Query by unique API identifier
     */
    Optional<ApiDefinition> findByApiCode(String apiCode);

    /**
     * Query by tenant ID + flow ID
     */
    Optional<ApiDefinition> findByTenantIdAndFlowCode(String tenantId, String flowCode);
}