package fr.ses10doigts.toolkitbridge.model.dto.tool.memory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemoryRulePayload {
    private Long id;
    private String scope;
    private String scopeId;
    private String title;
    private String content;
    private String priority;
    private String status;
    private String writeMode;
}
