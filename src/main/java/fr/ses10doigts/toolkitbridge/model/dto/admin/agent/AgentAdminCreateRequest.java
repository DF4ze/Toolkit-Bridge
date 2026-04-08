package fr.ses10doigts.toolkitbridge.model.dto.admin.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentAdminCreateRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[a-zA-Z0-9._-]+")
        String agentId
) {
}
