package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LlmAdminFacade {

    private final AdministrableConfigurationGateway configurationGateway;

    public List<LlmAdminResponse> listLlms() {
        return configurationGateway.loadOpenAiLikeProviders().stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<LlmAdminResponse> getLlm(String llmId) {
        if (llmId == null || llmId.isBlank()) {
            return Optional.empty();
        }

        return listLlms().stream()
                .filter(llm -> llm.llmId().equals(llmId))
                .findFirst();
    }

    private LlmAdminResponse toResponse(OpenAiLikeProperties provider) {
        return new LlmAdminResponse(
                provider.name(),
                provider.baseUrl(),
                provider.defaultModel(),
                !isBlank(provider.apiKey())
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
