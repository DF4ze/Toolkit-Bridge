package fr.ses10doigts.toolkitbridge.service.llm;

public interface LlmService {
    String chat(String providerName, String model, String systemPrompt, String userMessage, boolean toolsEnabled);
}
