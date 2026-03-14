package fr.ses10doigts.toolkitbridge.model.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaChatResponse(
        String model,
        @JsonProperty("created_at") String createdAt,
        OllamaMessage message,
        boolean done
) {
}
