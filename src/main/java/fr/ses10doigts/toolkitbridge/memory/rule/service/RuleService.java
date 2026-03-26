package fr.ses10doigts.toolkitbridge.memory.rule.service;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;

import java.util.List;

public interface RuleService {

    RuleEntry create(RuleEntry entry);

    RuleEntry update(Long id, RuleEntry updated);

    void activate(Long id);

    void deactivate(Long id);

    List<RuleEntry> getApplicableRules(String agentId, String projectId);
}
