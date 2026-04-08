package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        String normalizedLlmId = normalize(llmId);
        if (normalizedLlmId == null) {
            return Optional.empty();
        }

        return configurationGateway.loadOpenAiLikeProviders().stream()
                .filter(provider -> normalizedLlmId.equals(normalize(provider.name())))
                .map(this::toResponse)
                .findFirst();
    }

    public LlmAdminResponse createLlm(
            String llmId,
            String baseUrl,
            String defaultModel,
            String apiKey
    ) {
        String normalizedLlmId = requireNonBlank(llmId, "llmId");
        String normalizedBaseUrl = requireNonBlank(baseUrl, "baseUrl");
        String normalizedDefaultModel = requireNonBlank(defaultModel, "defaultModel");
        String normalizedApiKey = normalize(apiKey);

        List<OpenAiLikeProperties> providers = new ArrayList<>(configurationGateway.loadOpenAiLikeProviders());
        boolean alreadyExists = providers.stream()
                .anyMatch(provider -> normalizedLlmId.equals(normalize(provider.name())));
        if (alreadyExists) {
            throw new LlmAdminValidationException("A provider with this id already exists.");
        }

        providers.add(new OpenAiLikeProperties(
                normalizedLlmId,
                normalizedBaseUrl,
                normalizedApiKey,
                normalizedDefaultModel
        ));
        configurationGateway.saveOpenAiLikeProviders(providers);
        return toResponse(providers.getLast());
    }

    public Optional<LlmAdminResponse> updateLlm(
            String llmId,
            String baseUrl,
            String defaultModel,
            String apiKey
    ) {
        String normalizedLlmId = requireNonBlank(llmId, "llmId");
        String normalizedBaseUrl = requireNonBlank(baseUrl, "baseUrl");
        String normalizedDefaultModel = requireNonBlank(defaultModel, "defaultModel");
        String normalizedApiKey = normalize(apiKey);

        List<OpenAiLikeProperties> providers = new ArrayList<>(configurationGateway.loadOpenAiLikeProviders());
        for (int i = 0; i < providers.size(); i++) {
            OpenAiLikeProperties existing = providers.get(i);
            if (!normalizedLlmId.equals(normalize(existing.name()))) {
                continue;
            }

            String resolvedApiKey = normalizedApiKey == null ? existing.apiKey() : normalizedApiKey;
            OpenAiLikeProperties updated = new OpenAiLikeProperties(
                    normalizedLlmId,
                    normalizedBaseUrl,
                    resolvedApiKey,
                    normalizedDefaultModel
            );
            providers.set(i, updated);
            configurationGateway.saveOpenAiLikeProviders(providers);
            return Optional.of(toResponse(updated));
        }

        return Optional.empty();
    }

    private LlmAdminResponse toResponse(OpenAiLikeProperties provider) {
        return new LlmAdminResponse(
                provider.name(),
                provider.baseUrl(),
                provider.defaultModel(),
                !isBlank(provider.apiKey())
        );
    }

    private String requireNonBlank(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new LlmAdminValidationException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
