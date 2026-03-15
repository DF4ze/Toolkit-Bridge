package fr.ses10doigts.toolkitbridge.model.dto.llm.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class Message{
    private final MessageRole role;
    private final String content;
}
