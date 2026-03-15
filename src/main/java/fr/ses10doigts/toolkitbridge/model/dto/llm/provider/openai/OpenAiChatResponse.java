package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

import java.util.List;

public record OpenAiChatResponse(
        String id,
        String model,
        List<OpenAiChoice> choices
) {
}