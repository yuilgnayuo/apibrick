package com.citi.tts.apibrick.service.service;

import com.citi.tts.apibrick.common.util.JsonUtil;
import com.citi.tts.apibrick.service.domain.ApiExecuteFlow;
import com.citi.tts.apibrick.service.repository.ApiExecuteFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Business service for ApiExecuteFlow
 */
@Service
@RequiredArgsConstructor
public class ApiExecuteFlowService {

    private final ApiExecuteFlowRepository flowRepository;

    /**
     * Build ApiExecuteFlow after orchestration JSON parsing and persist it
     */
    public ApiExecuteFlow saveFromFlowJson(String tenantId, String flowCode, String flowName, String flowJson) {
        ApiExecuteFlow flow = JsonUtil.parse(flowJson, ApiExecuteFlow.class);
        flow.setTenantId(tenantId);
        flow.setFlowCode(flowCode);
        flow.setFlowName(flowName);
        return flowRepository.save(flow);
    }
}