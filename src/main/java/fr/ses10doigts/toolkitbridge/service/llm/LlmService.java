package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;

import java.util.List;

public interface LlmService {
    String chat(String providerName, String model, String systemPrompt, String userMessage, List<ToolDefinition> toolDefinitions);
}
