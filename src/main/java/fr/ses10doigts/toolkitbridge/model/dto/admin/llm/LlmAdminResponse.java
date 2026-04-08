package fr.ses10doigts.toolkitbridge.model.dto.admin.llm;

public record LlmAdminResponse(
        String llmId,
        String baseUrl,
        String defaultModel,
        boolean apiKeyConfigured
) {
}
