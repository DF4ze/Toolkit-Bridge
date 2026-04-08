package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void createLlmPersistsNewProvider() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade facade = new LlmAdminFacade(gateway);

        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "secret", "gpt-5")
        ));

        LlmAdminResponse created = facade.createLlm("mistral", "https://mistral.example/v1", "mistral-large", "new-key");

        assertThat(created).isEqualTo(new LlmAdminResponse("mistral", "https://mistral.example/v1", "mistral-large", true));
        verify(gateway).saveOpenAiLikeProviders(eq(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "secret", "gpt-5"),
                new OpenAiLikeProperties("mistral", "https://mistral.example/v1", "new-key", "mistral-large")
        )));
    }

    @Test
    void createLlmRejectsDuplicateId() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade facade = new LlmAdminFacade(gateway);

        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "secret", "gpt-5")
        ));

        assertThatThrownBy(() -> facade.createLlm("openai", "https://other/v1", "gpt-x", ""))
                .isInstanceOf(LlmAdminValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateLlmKeepsExistingApiKeyWhenInputIsBlank() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade facade = new LlmAdminFacade(gateway);

        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "secret", "gpt-5")
        ));

        Optional<LlmAdminResponse> updated = facade.updateLlm("openai", "https://api.openai.com/v2", "gpt-5.1", " ");

        assertThat(updated).contains(new LlmAdminResponse("openai", "https://api.openai.com/v2", "gpt-5.1", true));
        verify(gateway).saveOpenAiLikeProviders(eq(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v2", "secret", "gpt-5.1")
        )));
    }
}
