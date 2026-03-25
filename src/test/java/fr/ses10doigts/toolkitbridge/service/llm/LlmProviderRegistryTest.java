package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.LlmCapability;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.ModelInfo;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LlmProviderRegistryTest {

    @Test
    void shouldRegisterProvidersAndRetrieveThemByName() {
        LlmProvider p1 = new FakeProvider("openai");
        LlmProvider p2 = new FakeProvider("ollama");

        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(p1, p2));

        assertSame(p1, registry.getRequired("openai"));
        assertSame(p2, registry.getRequired("ollama"));
        assertTrue(registry.exists("openai"));
        assertFalse(registry.exists("missing"));
    }


    @Test
    @Disabled("Ordering should not be part of the registry contract")
    void shouldKeepInsertionOrderInGetAll() {
        LlmProvider p1 = new FakeProvider("first");
        LlmProvider p2 = new FakeProvider("second");

        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(p1, p2));

        assertEquals(List.of(p1, p2), registry.getAll());
    }

    @Test
    void shouldRejectBlankProviderName() {
        LlmProvider blank = new FakeProvider("   ");

        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> new LlmProviderRegistry(List.of(blank))
        );

        assertTrue(ex.getMessage().contains("blank name"));
    }

    @Test
    void shouldRejectDuplicateProviderNames() {
        LlmProvider p1 = new FakeProvider("same");
        LlmProvider p2 = new FakeProvider("same");

        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> new LlmProviderRegistry(List.of(p1, p2))
        );

        assertTrue(ex.getMessage().contains("Duplicate provider name: same"));
    }

    @Test
    void shouldThrowWhenProviderDoesNotExist() {
        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(new FakeProvider("openai")));

        LlmProviderException ex = assertThrows(
                LlmProviderException.class,
                () -> registry.getRequired("missing")
        );

        assertTrue(ex.getMessage().contains("Unknown provider: missing"));
    }

    @Test
    void getAllShouldBeUnmodifiable() {
        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(new FakeProvider("openai")));

        List<LlmProvider> providers = registry.getAll();

        assertThrows(UnsupportedOperationException.class, () -> providers.add(new FakeProvider("x")));
    }

    private static final class FakeProvider implements LlmProvider {
        private final String name;

        private FakeProvider(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public Set<LlmCapability> getCapabilities() { return Set.of(); }
        @Override public ChatResponse chat(ChatRequest request) { throw new UnsupportedOperationException(); }
        @Override public List<ModelInfo> listModels() { return List.of(); }
    }
}
