package com.growthtrace.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsConfiguredAiProperties() {
        contextRunner.withPropertyValues(
                "growthtrace.ai.provider=openai-compatible",
                "growthtrace.ai.base-url=https://api.example.com/v1",
                "growthtrace.ai.api-key=sk-test",
                "growthtrace.ai.model=gpt-test",
                "growthtrace.ai.connect-timeout-seconds=12",
                "growthtrace.ai.timeout-seconds=95",
                "growthtrace.ai.temperature=0.35"
        ).run(context -> {
            assertThat(context).hasSingleBean(AiProperties.class);
            AiProperties props = context.getBean(AiProperties.class);
            assertThat(props.provider()).isEqualTo("openai-compatible");
            assertThat(props.baseUrl()).isEqualTo("https://api.example.com/v1");
            assertThat(props.apiKey()).isEqualTo("sk-test");
            assertThat(props.model()).isEqualTo("gpt-test");
            assertThat(props.connectTimeoutSeconds()).isEqualTo(12);
            assertThat(props.timeoutSeconds()).isEqualTo(95);
            assertThat(props.temperature()).isEqualTo(0.35d);
        });
    }

    @Configuration
    @EnableConfigurationProperties(AiProperties.class)
    static class TestConfig {
    }
}
