package com.growthtrace.ai.adapter;

import com.growthtrace.common.enums.AiScenario;
import com.growthtrace.common.exception.AiException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiCompatibleProviderTest {

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(new AiProperties(
                "openai-compatible",
                "https://api.example.com/v1",
                "",
                "gpt-test",
                5,
                30,
                0.2
        ));

        AiException ex = assertThrows(AiException.class,
                () -> provider.chat(AiScenario.PROFILE_EXTRACT, "{}", Duration.ofSeconds(1)));

        assertThat(ex.getCode()).isEqualTo(ResultCode.AI_UNAVAILABLE);
        assertThat(ex.getMessage()).contains("apiKey");
    }

    @Test
    void rejectsMissingBaseUrlBeforeSendingRequest() {
        OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(new AiProperties(
                "openai-compatible",
                "",
                "sk-test",
                "gpt-test",
                5,
                30,
                0.2
        ));

        AiException ex = assertThrows(AiException.class,
                () -> provider.chat(AiScenario.PROFILE_EXTRACT, "{}", Duration.ofSeconds(1)));

        assertThat(ex.getCode()).isEqualTo(ResultCode.AI_UNAVAILABLE);
        assertThat(ex.getMessage()).contains("baseUrl");
    }

    @Test
    void throwsUnavailableWhenProviderReturnsNon2xx() throws Exception {
        withServer(503, "{\"error\":\"busy\"}", server -> {
            OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(new AiProperties(
                    "openai-compatible",
                    serverBaseUrl(server),
                    "sk-test",
                    "gpt-test",
                    5,
                    30,
                    0.2
            ));

            AiException ex = assertThrows(AiException.class,
                    () -> provider.chat(AiScenario.PROFILE_EXTRACT, "{}", Duration.ofSeconds(2)));

            assertThat(ex.getCode()).isEqualTo(ResultCode.AI_UNAVAILABLE);
            assertThat(ex.getMessage()).contains("HTTP 503");
        });
    }

    @Test
    void throwsParseErrorWhenResponseContentMissing() throws Exception {
        withServer(200, "{\"choices\":[{\"message\":{}}]}", server -> {
            OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(new AiProperties(
                    "openai-compatible",
                    serverBaseUrl(server),
                    "sk-test",
                    "gpt-test",
                    5,
                    30,
                    0.2
            ));

            AiException ex = assertThrows(AiException.class,
                    () -> provider.chat(AiScenario.PROFILE_EXTRACT, "{}", Duration.ofSeconds(2)));

            assertThat(ex.getCode()).isEqualTo(ResultCode.AI_PARSE_ERROR);
            assertThat(ex.getMessage()).contains("choices[0].message.content");
        });
    }

    @Test
    void throwsTimeoutWhenProviderRespondsTooSlowly() throws Exception {
        HttpServer server = createServer(exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        try {
            OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(new AiProperties(
                    "openai-compatible",
                    serverBaseUrl(server),
                    "sk-test",
                    "gpt-test",
                    5,
                    30,
                    0.2
            ));

            AiException ex = assertThrows(AiException.class,
                    () -> provider.chat(AiScenario.PROFILE_EXTRACT, "{}", Duration.ofMillis(50)));

            assertThat(ex.getCode()).isEqualTo(ResultCode.AI_TIMEOUT);
            assertThat(ex.getMessage()).contains("超时");
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private static void withServer(int status, String body, ThrowingConsumer<HttpServer> consumer) throws Exception {
        HttpServer server = createServer(exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        try {
            consumer.accept(server);
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer createServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
        return server;
    }

    private static String serverBaseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }
}
