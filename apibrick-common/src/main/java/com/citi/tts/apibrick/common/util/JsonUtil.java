package com.citi.tts.apibrick.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * JSON Utility Class
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parse orchestration JSON string into ApiExecuteFlow model
     */
    public static <T> T parse(String flowJson, Class<T> clazz) {

        if (Objects.isNull(flowJson) || flowJson.isEmpty()) {
            throw new IllegalArgumentException("Flow json is empty");
        }
        try {
            return OBJECT_MAPPER.readValue(flowJson, clazz);
        } catch (Exception e) {
            log.error("Parse flow json to ApiExecuteFlow failed", e);
            throw new RuntimeException("Flow json parse error", e);
        }
    }

    public static String toStr(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
