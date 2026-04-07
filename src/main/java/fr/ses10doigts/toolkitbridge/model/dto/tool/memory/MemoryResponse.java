package fr.ses10doigts.toolkitbridge.model.dto.tool.memory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemoryResponse {
    private String operation;
    private String context;
    private MemoryFactPayload fact;
    private MemoryRulePayload rule;
}
