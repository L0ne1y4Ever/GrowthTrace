package com.growthtrace.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiConfigurationLogger {

    private final AiProperties aiProperties;

    @Bean
    public ApplicationRunner aiConfigurationSummaryRunner() {
        return args -> log.info(
                "AI config summary: provider={}, baseUrl={}, model={}, connectTimeoutSeconds={}, timeoutSeconds={}, apiKeyConfigured={}",
                blankAsUnknown(aiProperties.provider()),
                blankAsUnknown(aiProperties.baseUrl()),
                blankAsUnknown(aiProperties.model()),
                aiProperties.connectTimeoutSecondsOrDefault(),
                aiProperties.timeoutSecondsOrDefault(),
                StringUtils.hasText(aiProperties.apiKey())
        );
    }

    private static String blankAsUnknown(String value) {
        return StringUtils.hasText(value) ? value : "<unset>";
    }
}
