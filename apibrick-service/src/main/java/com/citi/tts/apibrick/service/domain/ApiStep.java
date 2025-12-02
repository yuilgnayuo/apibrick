package com.citi.tts.apibrick.service.domain;

import lombok.Data;

@Data
public class ApiStep {
    private String stepId;
    private String stepType;
    private String stepName;
    private String preStepIds;
    private boolean failStop;
    private Object stepConfig;
}
