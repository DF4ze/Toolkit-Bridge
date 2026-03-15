package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

import java.util.List;

public record OpenAiModelsResponse(
        List<OpenAiModelData> data
) {
}