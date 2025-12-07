package com.citi.tts.apibrick.service.service;

import com.citi.tts.apibrick.common.enums.ApiStatus;
import com.citi.tts.apibrick.common.enums.EncryptAlgorithm;
import com.citi.tts.apibrick.service.domain.ApiDefinition;
import com.citi.tts.apibrick.service.repository.ApiDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Business service for ApiDefinition
 */
@Service
@RequiredArgsConstructor
public class ApiDefinitionService {

    private final ApiDefinitionRepository apiDefinitionRepository;

    /**
     * Build ApiDefinition after Swagger parsing and persist it
     */
    public ApiDefinition saveFromSwagger(String tenantId, String apiCode, String apiName,
                                         String apiPath, String httpMethod,
                                         List<Map<String, Object>> requestParams,
                                         Map<String, Map<String, Object>> responseDef) {
        ApiDefinition apiDefinition = new ApiDefinition();
        apiDefinition.setTenantId(tenantId);
        apiDefinition.setApiCode(apiCode);
        apiDefinition.setApiName(apiName);
        apiDefinition.setApiPath(apiPath);
        apiDefinition.setHttpMethod(httpMethod);
        apiDefinition.setApiStatus(ApiStatus.DRAFT); // Initial status: Draft
        apiDefinition.setEncryptAlgorithm(EncryptAlgorithm.NONE); // No encryption by default
        apiDefinition.setDataSourceIds("ds_mysql_001"); // Associate data source
        apiDefinition.setRequestParams(requestParams);
        apiDefinition.setResponseDef(responseDef);
        apiDefinition.setFlowCode("flow_" + apiCode); // Associate orchestration flow

        return apiDefinitionRepository.save(apiDefinition);
    }
}