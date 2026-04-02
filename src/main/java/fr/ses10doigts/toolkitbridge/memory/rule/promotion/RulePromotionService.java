package fr.ses10doigts.toolkitbridge.memory.rule.promotion;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;

import java.util.List;

public interface RulePromotionService {

    List<RuleEntry> promote(MemoryContextRequest request, String text, String source);
}
