package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.agent.AgentsProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProvidersProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdministrableConfigurationGateway {

    private static final TypeReference<List<AgentDefinitionProperties>> AGENT_DEFINITIONS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<OpenAiLikeProperties>> OPENAI_LIKE_PROVIDERS_TYPE = new TypeReference<>() {
    };

    private final AdministrableConfigurationStoreService storeService;
    private final AgentsProperties agentsProperties;
    private final OpenAiLikeProvidersProperties openAiLikeProvidersProperties;

    public AdministrableConfigurationGateway(
            AdministrableConfigurationStoreService storeService,
            AgentsProperties agentsProperties,
            OpenAiLikeProvidersProperties openAiLikeProvidersProperties
    ) {
        this.storeService = storeService;
        this.agentsProperties = agentsProperties;
        this.openAiLikeProvidersProperties = openAiLikeProvidersProperties;
    }

    @Transactional(readOnly = true)
    public List<AgentDefinitionProperties> loadAgentDefinitions() {
        return storeService.read(AdministrableConfigKey.AGENT_DEFINITIONS, AGENT_DEFINITIONS_TYPE)
                .map(List::copyOf)
                .orElseGet(this::loadAgentDefinitionsSeed);
    }

    @Transactional(readOnly = true)
    public List<OpenAiLikeProperties> loadOpenAiLikeProviders() {
        return storeService.read(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS, OPENAI_LIKE_PROVIDERS_TYPE)
                .map(List::copyOf)
                .orElseGet(this::loadOpenAiLikeProvidersSeed);
    }

    @Transactional(readOnly = true)
    public List<AgentDefinitionProperties> loadAgentDefinitionsSeed() {
        List<AgentDefinitionProperties> definitions = agentsProperties.getDefinitions();
        return definitions == null ? List.of() : new ArrayList<>(definitions);
    }

    @Transactional(readOnly = true)
    public List<OpenAiLikeProperties> loadOpenAiLikeProvidersSeed() {
        List<OpenAiLikeProperties> providers = openAiLikeProvidersProperties.getProviders();
        return providers == null ? List.of() : new ArrayList<>(providers);
    }

    @Transactional
    public boolean bootstrapSeedsIfMissing() {
        boolean seededAgentDefinitions = bootstrapIfMissing(
                AdministrableConfigKey.AGENT_DEFINITIONS,
                loadAgentDefinitionsSeed(),
                AGENT_DEFINITIONS_TYPE
        );
        boolean seededOpenAiProviders = bootstrapIfMissing(
                AdministrableConfigKey.OPENAI_LIKE_PROVIDERS,
                loadOpenAiLikeProvidersSeed(),
                OPENAI_LIKE_PROVIDERS_TYPE
        );
        return seededAgentDefinitions || seededOpenAiProviders;
    }

    private <T> boolean bootstrapIfMissing(AdministrableConfigKey key, T seedValue, TypeReference<T> type) {
        if (storeService.read(key, type).isPresent()) {
            return false;
        }
        storeService.write(key, seedValue);
        return true;
    }
}
