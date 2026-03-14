package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;

import java.util.List;

public interface OllamaClient {

    OllamaChatResponse chat(List<OllamaMessage> messages, List<OllamaToolDefinition> tools);

    OllamaChatResponse chatWithToolResults(List<OllamaMessage> conversation, List<OllamaToolDefinition> tools);
}
