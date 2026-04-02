package fr.ses10doigts.toolkitbridge.memory.semantic.extractor;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSemanticMemoryExtractorTest {

    private final DefaultSemanticMemoryExtractor extractor = new DefaultSemanticMemoryExtractor();

    @Test
    void storesDurablePreference() {
        MemoryContextRequest request = request();

        List<MemoryEntry> result = extractor.extract(request, "L'utilisateur prefere YAML pour la configuration.", "user");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getType()).isEqualTo(MemoryType.PREFERENCE);
        assertThat(result.getFirst().getContent()).contains("prefere YAML");
    }

    @Test
    void ignoresEphemeralInstruction() {
        MemoryContextRequest request = request();

        List<MemoryEntry> result = extractor.extract(request, "Fais-moi un resume aujourd'hui.", "user");

        assertThat(result).isEmpty();
    }

    @Test
    void storesStableConvention() {
        MemoryContextRequest request = request();

        List<MemoryEntry> result = extractor.extract(request, "Les noms de classes sont en anglais.", "user");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getType()).isEqualTo(MemoryType.CONTEXT);
        assertThat(result.getFirst().getContent()).contains("noms de classes");
    }

    private MemoryContextRequest request() {
        return new MemoryContextRequest("agent-1", "user-1", "bot-1", null, "hello", "conv-1", null, null, null, null);
    }
}
