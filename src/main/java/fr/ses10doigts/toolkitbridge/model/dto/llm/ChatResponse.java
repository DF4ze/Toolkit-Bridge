package fr.ses10doigts.toolkitbridge.model.dto.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;

public record ChatResponse(
        String model,
        Message message
) {
}
