package com.growthtrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "growthtrace.ai")
public record AiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        Integer timeoutSeconds,
        Double temperature
) {
    public int timeoutSecondsOrDefault() {
        return timeoutSeconds == null ? 30 : timeoutSeconds;
    }

    public double temperatureOrDefault() {
        return temperature == null ? 0.2d : temperature;
    }
}
