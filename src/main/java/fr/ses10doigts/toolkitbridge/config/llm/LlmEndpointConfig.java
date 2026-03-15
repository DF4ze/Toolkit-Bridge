package fr.ses10doigts.toolkitbridge.config.llm;

public record LlmEndpointConfig(
    String providerType,
    String name,
    String baseUrl,
    String apiKey,
    String model
) {}