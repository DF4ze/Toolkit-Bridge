package fr.ses10doigts.toolkitbridge.model.dto.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<OllamaToolCall> toolCalls
) {
}
