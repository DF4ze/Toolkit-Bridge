package fr.ses10doigts.toolkitbridge.service.llm.debug;

import java.time.Instant;

public record LlmDebugSnapshot(
        String agentId,
        String provider,
        String model,
        boolean toolsEnabled,
        String traceId,
        Instant timestamp,
        String systemPrompt,
        String userMessage,
        String response,
        String error
) {
}
