package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolSpec;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ToolDescriptor(
        String name,
        ToolKind kind,
        ToolCategory category,
        String description,
        Map<String, Object> parametersSchema,
        Set<ToolCapability> capabilities,
        ToolRiskLevel riskLevel
) {

    public ToolDescriptor {
        name = normalizeName(name);
        kind = Objects.requireNonNull(kind, "kind must not be null");
        category = Objects.requireNonNull(category, "category must not be null");
        description = description == null ? "" : description;
        parametersSchema = parametersSchema == null ? Map.of() : Map.copyOf(parametersSchema);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
    }

    public boolean scripted() {
        return kind == ToolKind.SCRIPTED;
    }

    public boolean requiresWebAccess() {
        return capabilities.contains(ToolCapability.WEB_ACCESS);
    }

    public ToolDefinition toToolDefinition() {
        return ToolDefinition.function(
                new ToolSpec(
                        name,
                        description,
                        parametersSchema
                )
        );
    }

    private static String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("tool name must not be null");
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        return normalized;
    }
}
