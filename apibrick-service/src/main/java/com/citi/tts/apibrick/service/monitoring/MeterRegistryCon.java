package com.citi.tts.apibrick.service.monitoring;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MeterRegistryCon {

    @Bean
    public MeterRegistry registry(){
        return new CompositeMeterRegistry();
    }
}
