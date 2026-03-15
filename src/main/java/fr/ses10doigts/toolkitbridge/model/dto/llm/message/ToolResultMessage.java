package fr.ses10doigts.toolkitbridge.model.dto.llm.message;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResultMessage extends Message{

    private final String toolName;
    private final String toolCallId;

    public ToolResultMessage(String content, String toolName, String toolCallId){
        super(MessageRole.TOOL, content);
        this.toolName = toolName;
        this.toolCallId = toolCallId;
    }
}
