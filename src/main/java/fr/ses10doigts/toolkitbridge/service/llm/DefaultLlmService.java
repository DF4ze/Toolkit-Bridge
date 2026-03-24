package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.MessageRole;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultLlmService implements LlmService {

    private final LlmProviderRegistry llmProviderRegistry;
    private final ToolRegistryService toolRegistryService;


    @Override
    public String chat(String providerName, String model, String systemPrompt, String userMessage) {
        LlmProvider provider = llmProviderRegistry.getRequired(providerName);
        List<ToolDefinition> toolDefinitions = toolRegistryService.getToolDefinitions();

        List<Message> messages = List.of(
                new Message(MessageRole.SYSTEM, systemPrompt),
                new Message(MessageRole.USER, userMessage)
        );

        ChatRequest request = new ChatRequest(
                model,
                messages,
                toolDefinitions
                );

        ChatResponse response = provider.chat(request);

        return response.message().getContent();
    }
}