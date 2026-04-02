package fr.ses10doigts.toolkitbridge.memory.rule.promotion;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DefaultRulePromotionService implements RulePromotionService {

    private static final Set<String> EXPLICIT_MARKERS = Set.of(
            "toujours", "dorenavant", "a partir de maintenant",
            "ne fais jamais", "utilise systematiquement", "always", "never"
    );

    @Override
    public List<RuleEntry> promote(MemoryContextRequest request, String text, String source) {
        if (request == null || text == null || text.isBlank()) {
            return List.of();
        }

        List<RuleEntry> promoted = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();

        for (String sentence : splitSentences(text)) {
            String normalized = sentence.toLowerCase(Locale.ROOT);
            if (EXPLICIT_MARKERS.stream().noneMatch(normalized::contains)) {
                continue;
            }
            String content = sentence.trim();
            String dedupeKey = content.toLowerCase(Locale.ROOT);
            if (!dedupe.add(dedupeKey)) {
                continue;
            }

            RuleEntry entry = new RuleEntry();
            entry.setAgentId(request.agentId());
            if (request.projectId() == null) {
                entry.setScope(RuleScope.AGENT);
                entry.setScopeId(null);
            } else {
                entry.setScope(RuleScope.PROJECT);
                entry.setScopeId(request.projectId());
            }
            entry.setStatus(RuleStatus.ACTIVE);
            entry.setPriority(RulePriority.HIGH);
            entry.setTitle(buildTitle(content));
            entry.setContent(content);
            promoted.add(entry);
        }

        return promoted;
    }

    private List<String> splitSentences(String text) {
        String[] parts = text.split("[\\n.!?]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String buildTitle(String content) {
        String prefix = "Promoted rule: ";
        if (content.length() <= 60) {
            return prefix + content;
        }
        return prefix + content.substring(0, 57) + "...";
    }
}