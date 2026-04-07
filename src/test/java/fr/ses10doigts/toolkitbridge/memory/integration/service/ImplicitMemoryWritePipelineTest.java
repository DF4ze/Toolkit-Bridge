package fr.ses10doigts.toolkitbridge.memory.integration.service;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.promotion.RulePromotionService;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.extractor.SemanticMemoryExtractor;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImplicitMemoryWritePipelineTest {

    private SemanticMemoryExtractor semanticMemoryExtractor;
    private SemanticMemoryService semanticMemoryService;
    private RulePromotionService rulePromotionService;
    private RuleService ruleService;
    private ImplicitMemoryWritePipeline pipeline;

    @BeforeEach
    void setUp() {
        semanticMemoryExtractor = mock(SemanticMemoryExtractor.class);
        semanticMemoryService = mock(SemanticMemoryService.class);
        rulePromotionService = mock(RulePromotionService.class);
        ruleService = mock(RuleService.class);

        pipeline = new ImplicitMemoryWritePipeline(
                semanticMemoryExtractor,
                semanticMemoryService,
                rulePromotionService,
                ruleService
        );
    }

    @Test
    void semanticExtractionsArePersistedAsImplicitWrites() {
        MemoryEntry extracted = new MemoryEntry();
        extracted.setAgentId("agent-1");
        extracted.setScope(MemoryScope.AGENT);
        extracted.setType(MemoryType.FACT);
        extracted.setContent("The assistant prefers concise answers.");

        when(semanticMemoryExtractor.extract(any(), any(), any())).thenReturn(List.of(extracted));
        when(semanticMemoryService.search("agent-1", "The assistant prefers concise answers.")).thenReturn(List.of());

        pipeline.persistSemanticExtractions(request(), "text", "assistant");

        ArgumentCaptor<MemoryEntry> captor = ArgumentCaptor.forClass(MemoryEntry.class);
        verify(semanticMemoryService).create(captor.capture());
        assertThat(captor.getValue().getWriteMode()).isEqualTo(MemoryWriteMode.IMPLICIT);
    }

    @Test
    void promotedRulesArePersistedAsImplicitWrites() {
        RuleEntry promoted = new RuleEntry();
        promoted.setAgentId("agent-1");
        promoted.setScope(RuleScope.AGENT);
        promoted.setTitle("Promoted");
        promoted.setContent("Always add tests.");
        promoted.setPriority(RulePriority.HIGH);

        when(rulePromotionService.promote(any(), any(), any())).thenReturn(List.of(promoted));
        when(ruleService.getApplicableRules("agent-1", "project-1")).thenReturn(List.of());

        pipeline.promoteRules(request(), "text", "assistant");

        ArgumentCaptor<RuleEntry> captor = ArgumentCaptor.forClass(RuleEntry.class);
        verify(ruleService).create(captor.capture());
        assertThat(captor.getValue().getWriteMode()).isEqualTo(MemoryWriteMode.IMPLICIT);
    }

    private MemoryContextRequest request() {
        return new MemoryContextRequest(
                "agent-1",
                "user-1",
                null,
                "project-1",
                "hello",
                "conversation-1",
                null,
                null,
                null,
                null
        );
    }
}
