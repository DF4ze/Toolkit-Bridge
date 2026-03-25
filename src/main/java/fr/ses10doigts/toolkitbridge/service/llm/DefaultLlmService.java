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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultLlmService implements LlmService {

    private final LlmProviderRegistry llmProviderRegistry;
    private final ToolRegistryService toolRegistryService;


    @Override
    public String chat(String providerName, String model, String systemPrompt, String userMessage) {
        long startNanos = System.nanoTime();
        LlmProvider provider = llmProviderRegistry.getRequired(providerName);
        List<ToolDefinition> toolDefinitions = toolRegistryService.getToolDefinitions();

        log.debug("LLM request build provider={} model={} tools={} systemPromptLength={} userMessageLength={}",
                providerName,
                model,
                toolDefinitions.size(),
                systemPrompt == null ? 0 : systemPrompt.length(),
                userMessage == null ? 0 : userMessage.length());

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

        String content = response.message().getContent();
        log.info("LLM request completed provider={} model={} responseLength={} durationMs={}",
                providerName,
                model,
                content == null ? 0 : content.length(),
                elapsedMs(startNanos));
        return content;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
