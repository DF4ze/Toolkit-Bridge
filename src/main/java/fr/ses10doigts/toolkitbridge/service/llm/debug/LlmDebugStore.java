package fr.ses10doigts.toolkitbridge.service.llm.debug;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class LlmDebugStore {

    private static final int MAX_FIELD_LENGTH = 8_000;
    private static final Pattern OPENAI_KEY = Pattern.compile("\\bsk-[A-Za-z0-9]{20,}\\b");
    private static final Pattern TELEGRAM_TOKEN = Pattern.compile("\\b\\d{9,}:[A-Za-z0-9_-]{20,}\\b");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)bearer\\s+\\S+");
    private static final Pattern API_KEY_LINE = Pattern.compile("(?i)api[-_ ]?key\\s*[:=]\\s*\\S+");

    private final Map<String, LlmDebugSnapshot> snapshots = new ConcurrentHashMap<>();

    public void recordSuccess(String agentId,
                              String provider,
                              String model,
                              boolean toolsEnabled,
                              String traceId,
                              String systemPrompt,
                              String userMessage,
                              String response) {
        LlmDebugSnapshot snapshot = new LlmDebugSnapshot(
                agentId,
                provider,
                model,
                toolsEnabled,
                traceId,
                Instant.now(),
                sanitize(systemPrompt),
                sanitize(userMessage),
                sanitize(response),
                null
        );
        snapshots.put(agentId, snapshot);
    }

    public void recordFailure(String agentId,
                              String provider,
                              String model,
                              boolean toolsEnabled,
                              String traceId,
                              String systemPrompt,
                              String userMessage,
                              String error) {
        LlmDebugSnapshot snapshot = new LlmDebugSnapshot(
                agentId,
                provider,
                model,
                toolsEnabled,
                traceId,
                Instant.now(),
                sanitize(systemPrompt),
                sanitize(userMessage),
                null,
                sanitize(error)
        );
        snapshots.put(agentId, snapshot);
    }

    public Optional<LlmDebugSnapshot> get(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshots.get(agentId));
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value;
        sanitized = OPENAI_KEY.matcher(sanitized).replaceAll("sk-***");
        sanitized = TELEGRAM_TOKEN.matcher(sanitized).replaceAll("telegram-token-***");
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("Bearer ***");
        sanitized = API_KEY_LINE.matcher(sanitized).replaceAll("api-key: ***");

        if (sanitized.length() > MAX_FIELD_LENGTH) {
            return sanitized.substring(0, MAX_FIELD_LENGTH) + "...[truncated]";
        }

        return sanitized;
    }
}
