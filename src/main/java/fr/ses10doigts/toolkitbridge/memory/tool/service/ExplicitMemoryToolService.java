package fr.ses10doigts.toolkitbridge.memory.tool.service;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitFactMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitRuleMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.MemoryContextRecallRequest;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ExplicitMemoryToolService {

    private static final String DEFAULT_CONTEXT_FOCUS = "Explicit memory context recall.";
    private static final double DEFAULT_FACT_IMPORTANCE = 0.8d;
    private static final String USER_INPUT_SECTION_MARKER = "\n\n## User Input\n";

    private final MemoryFacade memoryFacade;
    private final SemanticMemoryService semanticMemoryService;
    private final RuleService ruleService;
    private final MemoryScopePolicy memoryScopePolicy;
    private final AgentPermissionControlService permissionControlService;

    @Autowired
    public ExplicitMemoryToolService(
            MemoryFacade memoryFacade,
            SemanticMemoryService semanticMemoryService,
            RuleService ruleService,
            MemoryScopePolicy memoryScopePolicy,
            AgentPermissionControlService permissionControlService
    ) {
        this.memoryFacade = memoryFacade;
        this.semanticMemoryService = semanticMemoryService;
        this.ruleService = ruleService;
        this.memoryScopePolicy = memoryScopePolicy;
        this.permissionControlService = permissionControlService;
    }

    public MemoryContext recallContext(MemoryContextRecallRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        checkRecallScopes(request);

        MemoryContextRequest memoryRequest = new MemoryContextRequest(
                requireText(request.agentId(), "agentId"),
                request.userId(),
                null,
                request.projectId(),
                defaultIfBlank(request.focus(), DEFAULT_CONTEXT_FOCUS),
                request.conversationId(),
                request.maxSemanticMemories(),
                request.maxEpisodes(),
                null,
                null
        );

        MemoryContext memoryContext = memoryFacade.buildContext(memoryRequest);
        return new MemoryContext(
                stripSyntheticUserInputSection(memoryContext.text()),
                memoryContext.injectedSemanticMemoryIds()
        );
    }

    public MemoryEntry writeFact(ExplicitFactMemoryWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        MemoryScope scope = resolveFactScope(request);
        permissionControlService.checkMemoryScopeAccess(
                requireText(request.agentId(), "agentId"),
                mapFactScope(scope),
                "write_fact:" + scope.name()
        );
        String scopeId = resolveFactScopeId(request, scope);

        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId(requireText(request.agentId(), "agentId"));
        entry.setScope(scope);
        entry.setScopeId(scopeId);
        entry.setType(request.type() == null ? MemoryType.FACT : request.type());
        entry.setContent(requireText(request.content(), "content"));
        entry.setImportance(request.importance() == null ? DEFAULT_FACT_IMPORTANCE : request.importance());
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setWriteMode(MemoryWriteMode.EXPLICIT);
        entry.setTags(request.tags() == null ? Set.of() : new LinkedHashSet<>(request.tags()));

        if (request.memoryId() == null) {
            return semanticMemoryService.create(entry);
        }
        return semanticMemoryService.update(request.memoryId(), entry);
    }

    public RuleEntry writeRule(ExplicitRuleMemoryWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        RuleScope scope = resolveRuleScope(request);
        permissionControlService.checkMemoryScopeAccess(
                requireText(request.agentId(), "agentId"),
                mapRuleScope(scope),
                "write_rule:" + scope.name()
        );
        String scopeId = resolveRuleScopeId(request, scope);

        RuleEntry entry = new RuleEntry();
        entry.setAgentId(scope == RuleScope.AGENT ? requireText(request.agentId(), "agentId") : null);
        entry.setScope(scope);
        entry.setScopeId(scopeId);
        entry.setTitle(requireText(request.title(), "title"));
        entry.setContent(requireText(request.content(), "content"));
        entry.setPriority(request.priority() == null ? RulePriority.HIGH : request.priority());
        entry.setStatus(RuleStatus.ACTIVE);
        entry.setWriteMode(MemoryWriteMode.EXPLICIT);

        if (request.ruleId() == null) {
            return ruleService.create(entry);
        }
        return ruleService.update(request.ruleId(), entry);
    }

    private MemoryScope resolveFactScope(ExplicitFactMemoryWriteRequest request) {
        if (request.scope() != null) {
            return request.scope();
        }
        return memoryScopePolicy.resolveDurableWriteScope(request.userId(), request.projectId());
    }

    private String resolveFactScopeId(ExplicitFactMemoryWriteRequest request, MemoryScope scope) {
        if (hasText(request.scopeId())) {
            return request.scopeId().trim();
        }
        return memoryScopePolicy.resolveScopeId(scope, request.userId(), request.projectId());
    }

    private RuleScope resolveRuleScope(ExplicitRuleMemoryWriteRequest request) {
        if (request.scope() != null) {
            return request.scope();
        }
        return hasText(request.projectId()) ? RuleScope.PROJECT : RuleScope.AGENT;
    }

    private String resolveRuleScopeId(ExplicitRuleMemoryWriteRequest request, RuleScope scope) {
        if (hasText(request.scopeId())) {
            return request.scopeId().trim();
        }
        if (scope == RuleScope.PROJECT) {
            return normalize(request.projectId());
        }
        return null;
    }

    private String requireText(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private boolean hasText(String value) {
        return normalize(value) != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stripSyntheticUserInputSection(String contextText) {
        if (contextText == null || contextText.isBlank()) {
            return contextText;
        }
        int markerIndex = contextText.indexOf(USER_INPUT_SECTION_MARKER);
        if (markerIndex < 0) {
            return contextText;
        }
        return contextText.substring(0, markerIndex);
    }

    private void checkRecallScopes(MemoryContextRecallRequest request) {
        String agentId = requireText(request.agentId(), "agentId");
        permissionControlService.checkMemoryScopeAccess(agentId, AgentMemoryScope.AGENT, "recall_context:AGENT");
        if (hasText(request.userId())) {
            permissionControlService.checkMemoryScopeAccess(agentId, AgentMemoryScope.USER, "recall_context:USER");
        }
        if (hasText(request.projectId())) {
            permissionControlService.checkMemoryScopeAccess(agentId, AgentMemoryScope.PROJECT, "recall_context:PROJECT");
        }
    }

    private AgentMemoryScope mapFactScope(MemoryScope scope) {
        return switch (scope) {
            case AGENT -> AgentMemoryScope.AGENT;
            case USER -> AgentMemoryScope.USER;
            case PROJECT -> AgentMemoryScope.PROJECT;
            case SYSTEM, SHARED -> AgentMemoryScope.SYSTEM;
        };
    }

    private AgentMemoryScope mapRuleScope(RuleScope scope) {
        return switch (scope) {
            case AGENT -> AgentMemoryScope.AGENT;
            case PROJECT -> AgentMemoryScope.PROJECT;
            case GLOBAL -> AgentMemoryScope.GLOBAL;
        };
    }
}
