package fr.ses10doigts.toolkitbridge.model.dto.llm;


import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;

import java.util.List;

public record ChatRequest(
        String model,
        List<Message> messages,
        List<ToolDefinition> tools
) {
    public ChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
