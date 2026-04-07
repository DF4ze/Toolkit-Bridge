package fr.ses10doigts.toolkitbridge.model.dto.tool.memory;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class MemoryFactPayload {
    private Long id;
    private String scope;
    private String scopeId;
    private String type;
    private String content;
    private double importance;
    private String status;
    private String writeMode;
    private Set<String> tags;
}
