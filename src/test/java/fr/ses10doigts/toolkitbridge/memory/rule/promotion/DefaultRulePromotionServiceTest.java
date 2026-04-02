package fr.ses10doigts.toolkitbridge.memory.rule.promotion;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRulePromotionServiceTest {

    private final DefaultRulePromotionService service = new DefaultRulePromotionService();

    @Test
    void promotesExplicitRule() {
        MemoryContextRequest request = request();

        List<RuleEntry> rules = service.promote(request, "Toujours poser une question si ambigu.", "user");

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().getContent()).contains("Toujours poser une question");
    }

    @Test
    void ignoresVaguePreference() {
        MemoryContextRequest request = request();

        List<RuleEntry> rules = service.promote(request, "J'aime bien quand c'est clair.", "user");

        assertThat(rules).isEmpty();
    }

    private MemoryContextRequest request() {
        return new MemoryContextRequest("agent-1", "user-1", "bot-1", null, "hello", "conv-1", null, null, null, null);
    }
}
