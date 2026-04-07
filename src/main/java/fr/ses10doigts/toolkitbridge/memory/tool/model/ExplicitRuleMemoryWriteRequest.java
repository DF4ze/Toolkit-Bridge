package fr.ses10doigts.toolkitbridge.memory.tool.model;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;

public record ExplicitRuleMemoryWriteRequest(
        Long ruleId,
        String agentId,
        String projectId,
        RuleScope scope,
        String scopeId,
        String title,
        String content,
        RulePriority priority
) {
}
