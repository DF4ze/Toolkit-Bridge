package fr.ses10doigts.toolkitbridge.service.llm.provider;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

@Component
@Slf4j
public class ProviderHttpExecutor {

    public <T> T execute(String providerName, String endpoint, Supplier<T> call) {
        long startNanos = System.nanoTime();
        try {
            T response = call.get();

            if (response == null) {
                throw new LlmProviderException(buildPrefix(providerName, endpoint) + "empty response body");
            }

            return response;

        } catch (LlmProviderException e) {
            throw e;

        } catch (HttpStatusCodeException e) {
            throw new LlmProviderException(
                    buildPrefix(providerName, endpoint)
                            + "HTTP " + e.getStatusCode().value()
                            + " - " + safeBody(e.getResponseBodyAsString()),
                    e
            );

        } catch (ResourceAccessException e) {
            throw new LlmProviderException(
                    buildPrefix(providerName, endpoint)
                            + "provider unreachable: " + safeMessage(e),
                    e
            );

        } catch (Exception e) {
            throw new LlmProviderException(
                    buildPrefix(providerName, endpoint)
                            + "unexpected error: " + safeMessage(e),
                    e
            );
        } finally {
            log.debug("LLM provider call finished provider={} endpoint={} durationMs={}",
                    providerName,
                    endpoint,
                    elapsedMs(startNanos));
        }
    }

    private String buildPrefix(String providerName, String endpoint) {
        return "Provider '%s' [%s] - ".formatted(providerName, endpoint);
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "empty error body";
        }

        String trimmed = body.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable == null ? "unknown error" : throwable.getClass().getSimpleName();
        }
        return throwable.getMessage();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
