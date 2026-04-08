package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmAdminFacadeTest {

    @Test
    void mapsConfiguredProvidersToAdminDtos() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade facade = new LlmAdminFacade(gateway);

        OpenAiLikeProperties withApiKey = new OpenAiLikeProperties(
                "openai",
                "https://api.openai.com/v1",
                "secret",
                "gpt-5"
        );
        OpenAiLikeProperties withoutApiKey = new OpenAiLikeProperties(
                "local",
                "http://localhost:1234/v1",
                "  ",
                "qwen2.5"
        );

        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of(withApiKey, withoutApiKey));

        List<LlmAdminResponse> responses = facade.listLlms();
        assertThat(responses).containsExactly(
                new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true),
                new LlmAdminResponse("local", "http://localhost:1234/v1", "qwen2.5", false)
        );
    }

    @Test
    void getLlmReturnsEmptyForBlankOrUnknownId() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade facade = new LlmAdminFacade(gateway);

        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "secret", "gpt-5")
        ));

        assertThat(facade.getLlm("openai")).isEqualTo(Optional.of(
                new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)
        ));
        assertThat(facade.getLlm("missing")).isEmpty();
        assertThat(facade.getLlm(" ")).isEmpty();
    }
}
