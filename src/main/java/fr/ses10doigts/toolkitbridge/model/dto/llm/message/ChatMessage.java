package fr.ses10doigts.toolkitbridge.model.dto.llm.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tool.ToolCall;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage extends Message{

    private final @JsonProperty("tool_calls") List<ToolCall> toolCalls;

    public ChatMessage( MessageRole role, String content, List<ToolCall> toolCalls ){
        super( role, content);
        this.toolCalls = toolCalls;
    }
}
