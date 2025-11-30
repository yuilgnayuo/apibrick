package com.citi.tts.apibrick.service.tool;

import com.citi.tts.apibrick.common.util.JsonUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * JPA converter for JSON string and object (supports List/Map)
 */
@Slf4j
@Converter(autoApply = true)
public class JsonStringConverter implements AttributeConverter<Object, String> {

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (Objects.isNull(attribute)) {
            return null;
        }
        return JsonUtil.toStr(attribute);
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (Objects.isNull(dbData) || dbData.isEmpty()) {
            return null;
        }
        return JsonUtil.parse(dbData, Object.class);
    }
}