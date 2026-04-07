package fr.ses10doigts.toolkitbridge.memory.integration.service;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.promotion.RulePromotionService;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.extractor.SemanticMemoryExtractor;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ImplicitMemoryWritePipeline {

    private final SemanticMemoryExtractor semanticMemoryExtractor;
    private final SemanticMemoryService semanticMemoryService;
    private final RulePromotionService rulePromotionService;
    private final RuleService ruleService;

    public ImplicitMemoryWritePipeline(
            SemanticMemoryExtractor semanticMemoryExtractor,
            SemanticMemoryService semanticMemoryService,
            RulePromotionService rulePromotionService,
            RuleService ruleService
    ) {
        this.semanticMemoryExtractor = semanticMemoryExtractor;
        this.semanticMemoryService = semanticMemoryService;
        this.rulePromotionService = rulePromotionService;
        this.ruleService = ruleService;
    }

    public void persistSemanticExtractions(MemoryContextRequest request, String text, String source) {
        for (MemoryEntry candidate : semanticMemoryExtractor.extract(request, text, source)) {
            candidate.setWriteMode(MemoryWriteMode.IMPLICIT);
            if (alreadyExists(candidate)) {
                continue;
            }
            try {
                semanticMemoryService.create(candidate);
            } catch (Exception e) {
                log.warn("Unable to persist implicit semantic memory for agent={} content='{}'",
                        request.agentId(),
                        candidate.getContent(),
                        e);
            }
        }
    }

    public void promoteRules(MemoryContextRequest request, String text, String source) {
        List<RuleEntry> existing = safeApplicableRules(request);
        for (RuleEntry candidate : rulePromotionService.promote(request, text, source)) {
            candidate.setWriteMode(MemoryWriteMode.IMPLICIT);
            if (existing.stream().anyMatch(rule -> sameRule(rule, candidate))) {
                continue;
            }
            try {
                ruleService.create(candidate);
                existing.add(candidate);
            } catch (Exception e) {
                log.warn("Unable to persist implicit promoted rule for agent={} content='{}'",
                        request.agentId(),
                        candidate.getContent(),
                        e);
            }
        }
    }

    private List<RuleEntry> safeApplicableRules(MemoryContextRequest request) {
        try {
            return new ArrayList<>(ruleService.getApplicableRules(request.agentId(), request.projectId()));
        } catch (Exception e) {
            log.warn("Unable to load existing rules for agent={}", request.agentId(), e);
            return new ArrayList<>();
        }
    }

    private boolean alreadyExists(MemoryEntry candidate) {
        List<MemoryEntry> existing;
        try {
            existing = semanticMemoryService.search(candidate.getAgentId(), candidate.getContent());
        } catch (Exception e) {
            return false;
        }
        return existing.stream().anyMatch(entry -> sameMemory(entry, candidate));
    }

    private boolean sameMemory(MemoryEntry a, MemoryEntry b) {
        if (a == null || b == null) {
            return false;
        }
        return normalize(a.getContent()).equals(normalize(b.getContent()))
                && a.getScope() == b.getScope()
                && normalize(a.getScopeId()).equals(normalize(b.getScopeId()))
                && a.getType() == b.getType();
    }

    private boolean sameRule(RuleEntry a, RuleEntry b) {
        if (a == null || b == null) {
            return false;
        }
        return normalize(a.getContent()).equals(normalize(b.getContent()))
                && a.getScope() == b.getScope()
                && normalize(a.getScopeId()).equals(normalize(b.getScopeId()))
                && normalize(a.getAgentId()).equals(normalize(b.getAgentId()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
