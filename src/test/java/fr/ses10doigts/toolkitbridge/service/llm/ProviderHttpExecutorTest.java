package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProviderHttpExecutorTest {

    private final ProviderHttpExecutor executor = new ProviderHttpExecutor();

    @Test
    void shouldReturnResponseWhenCallSucceeds() {
        String result = executor.execute("lmstudio", "/models", () -> "ok");

        assertEquals("ok", result);
    }

    @Test
    void shouldFailWhenResponseBodyIsNull() {
        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> executor.execute("lmstudio", "/models", () -> null)
        );

        assertTrue(ex.getMessage().contains("Provider 'lmstudio' [/models] - empty response body"));
    }

    @Test
    void shouldRethrowExistingLlmProviderException() {
        LlmProviderException boom = new LlmProviderException("already wrapped");

        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> executor.execute("lmstudio", "/models", () -> { throw boom; })
        );

        assertSame(boom, ex);
    }

    @Test
    void shouldWrapHttpStatusCodeException() {
        HttpStatusCodeException httpEx =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        "{\"error\":\"bad payload\"}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                );

        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> executor.execute("lmstudio", "/chat/completions", () -> { throw httpEx; })
        );

        assertTrue(ex.getMessage().contains("HTTP 400"));
        assertTrue(ex.getMessage().contains("bad payload"));
    }

    @Test
    void shouldWrapResourceAccessException() {
        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> executor.execute("ollama", "/models", () -> {
                    throw new ResourceAccessException("Connection refused");
                })
        );

        assertTrue(ex.getMessage().contains("provider unreachable"));
        assertTrue(ex.getMessage().contains("Connection refused"));
    }

    @Test
    void shouldWrapUnexpectedException() {
        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> executor.execute("openai", "/models", () -> {
                    throw new IllegalStateException("boom");
                })
        );

        assertTrue(ex.getMessage().contains("unexpected error"));
        assertTrue(ex.getMessage().contains("boom"));
    }
}