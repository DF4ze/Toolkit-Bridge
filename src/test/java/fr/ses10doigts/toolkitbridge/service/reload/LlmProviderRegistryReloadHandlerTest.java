package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.service.llm.runtime.LlmProviderRegistryRuntime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderRegistryReloadHandlerTest {

    @Test
    void shouldReloadLlmProviderRegistryAndReturnSuccessResult() {
        LlmProviderRegistryRuntime runtime = mock(LlmProviderRegistryRuntime.class);
        when(runtime.reloadFromDatabase()).thenReturn(2);

        LlmProviderRegistryReloadHandler handler = new LlmProviderRegistryReloadHandler(runtime);

        ReloadDomainResult result = handler.reload();

        assertThat(result.domain()).isEqualTo(ReloadDomain.LLM_PROVIDER_REGISTRY);
        assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(result.message()).contains("providers=2");
    }
}
