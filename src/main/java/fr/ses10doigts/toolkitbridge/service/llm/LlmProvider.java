package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.LlmCapability;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.ModelInfo;

import java.util.List;
import java.util.Set;

public interface LlmProvider {
    String getName();
    Set<LlmCapability> getCapabilities();
    ChatResponse chat(ChatRequest request);
    List<ModelInfo> listModels();
}