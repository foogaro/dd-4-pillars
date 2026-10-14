package com.foogaro.hotel.common.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class ObservabilityConfig {

    private final ObjectProvider<OpenTelemetry> openTelemetry;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    public ObservabilityConfig(ObjectProvider<OpenTelemetry> openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @EventListener(ApplicationReadyEvent.class)
    void installLogbackAppender() {
        OpenTelemetry otel = openTelemetry.getIfAvailable();
        if (otel != null) {
            OpenTelemetryAppender.install(otel);
        }
    }

    @Bean
    ObservationFilter serviceObservationFilter() {
        return context -> {
            context.addLowCardinalityKeyValue(KeyValue.of("service.name", serviceName));
            return context;
        };
    }
}
