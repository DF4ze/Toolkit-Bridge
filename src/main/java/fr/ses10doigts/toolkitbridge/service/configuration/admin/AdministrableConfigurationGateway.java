package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;

@Service
public class AdministrableConfigurationGateway {

    private static final TypeReference<List<AgentDefinitionProperties>> AGENT_DEFINITIONS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<OpenAiLikeProperties>> OPENAI_LIKE_PROVIDERS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MemoryConfigurationPayload> MEMORY_CONFIGURATION_TYPE = new TypeReference<>() {
    };

    private final AdministrableConfigurationStoreService storeService;
    public AdministrableConfigurationGateway(
            AdministrableConfigurationStoreService storeService
    ) {
        this.storeService = storeService;
    }

    @Transactional(readOnly = true)
    public List<AgentDefinitionProperties> loadAgentDefinitions() {
        return storeService.read(AdministrableConfigKey.AGENT_DEFINITIONS, AGENT_DEFINITIONS_TYPE)
                .map(List::copyOf)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<OpenAiLikeProperties> loadOpenAiLikeProviders() {
        return storeService.read(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS, OPENAI_LIKE_PROVIDERS_TYPE)
                .map(List::copyOf)
                .orElse(List.of());
    }

    @Transactional
    public void saveAgentDefinitions(List<AgentDefinitionProperties> definitions) {
        List<AgentDefinitionProperties> value = definitions == null ? List.of() : List.copyOf(definitions);
        storeService.write(AdministrableConfigKey.AGENT_DEFINITIONS, value);
    }

    @Transactional
    public void saveOpenAiLikeProviders(List<OpenAiLikeProperties> providers) {
        List<OpenAiLikeProperties> value = providers == null ? List.of() : List.copyOf(providers);
        storeService.write(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS, value);
    }

    @Transactional(readOnly = true)
    public Optional<MemoryConfigurationPayload> loadMemoryConfiguration() {
        return storeService.read(AdministrableConfigKey.MEMORY_CONFIGURATION, MEMORY_CONFIGURATION_TYPE);
    }

    @Transactional
    public void saveMemoryConfiguration(MemoryConfigurationPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("memory configuration payload must not be null");
        }
        storeService.write(
                AdministrableConfigKey.MEMORY_CONFIGURATION,
                payload
        );
    }
}
