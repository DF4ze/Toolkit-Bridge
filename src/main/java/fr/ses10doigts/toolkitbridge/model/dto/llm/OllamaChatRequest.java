package fr.ses10doigts.toolkitbridge.model.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        List<OllamaToolDefinition> tools,
        boolean stream,
        @JsonProperty("keep_alive") String keepAlive
) {
}
