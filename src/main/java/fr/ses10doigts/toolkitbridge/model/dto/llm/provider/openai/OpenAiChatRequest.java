package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatRequest(
        String model,
        List<OpenAiMessage> messages,
        List<OpenAiToolDefinition> tools
) {
}