package fr.ses10doigts.toolkitbridge.service.tool.memory;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitRuleMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.service.ExplicitMemoryToolService;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WriteMemoryRuleToolHandler extends AbstractMemoryToolHandler {

    public static final String NAME = "write_memory_rule";

    private final ExplicitMemoryToolService memoryToolService;
    private final CurrentAgentService currentAgentService;

    public WriteMemoryRuleToolHandler(
            ExplicitMemoryToolService memoryToolService,
            CurrentAgentService currentAgentService
    ) {
        this.memoryToolService = memoryToolService;
        this.currentAgentService = currentAgentService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Create or update an explicit memory rule for the current agent or project.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "content"),
                "properties", Map.of(
                        "rule_id", Map.of("type", "integer"),
                        "project_id", Map.of("type", "string"),
                        "scope", Map.of("type", "string", "enum", List.of("AGENT", "PROJECT")),
                        "scope_id", Map.of("type", "string"),
                        "title", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "priority", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"))
                )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        Long ruleId = asLong(arguments.get("rule_id"));
        RuleEntry entry = memoryToolService.writeRule(new ExplicitRuleMemoryWriteRequest(
                ruleId,
                currentAgentId(),
                asString(arguments.get("project_id")),
                asRuleScope(arguments.get("scope")),
                asString(arguments.get("scope_id")),
                asString(arguments.get("title")),
                asString(arguments.get("content")),
                asRulePriority(arguments.get("priority"))
        ));

        String verb = ruleId == null ? "created" : "updated";
        return successWithRule("Memory rule " + verb + " successfully.", NAME, entry);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        return switch (value) {
            case null -> null;
            case Long longValue -> longValue;
            case Integer integer -> integer.longValue();
            default -> Long.parseLong(String.valueOf(value));
        };
    }

    private RuleScope asRuleScope(Object value) {
        return value == null ? null : RuleScope.valueOf(String.valueOf(value));
    }

    private RulePriority asRulePriority(Object value) {
        return value == null ? null : RulePriority.valueOf(String.valueOf(value));
    }

    private String currentAgentId() {
        return currentAgentService.getCurrentAgent().agentIdent();
    }
}
