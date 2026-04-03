package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import org.springframework.stereotype.Component;

@Component
public class OrchestrationResponseSanitizer {

    private static final int MAX_LLM_RESPONSE_LENGTH = 20_000;

    public String normalizeAssistantResponse(String response) {
        if (response == null) {
            return "";
        }

        String normalized = response.trim();
        if (normalized.length() > MAX_LLM_RESPONSE_LENGTH) {
            return normalized.substring(0, MAX_LLM_RESPONSE_LENGTH);
        }
        return normalized;
    }
}
