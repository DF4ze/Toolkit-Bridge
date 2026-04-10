package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminConfigQueryService {

    private final AgentDefinitionService agentDefinitionService;
    private final AdministrableConfigurationGateway configurationGateway;

    public AdminConfigQueryService(
            AgentDefinitionService agentDefinitionService,
            AdministrableConfigurationGateway configurationGateway
    ) {
        this.agentDefinitionService = agentDefinitionService;
        this.configurationGateway = configurationGateway;
    }

    public TechnicalAdminView.ConfigItem getConfigurationView() {
        List<AgentDefinition> definitions = agentDefinitionService.findAll();
        List<OpenAiLikeProperties> providers = configurationGateway.loadOpenAiLikeProviders();

        List<TechnicalAdminView.LlmProviderItem> providerItems = providers.stream()
                .map(provider -> new TechnicalAdminView.LlmProviderItem(
                        provider.name(),
                        provider.baseUrl(),
                        provider.defaultModel(),
                        !isBlank(provider.apiKey())
                ))
                .toList();

        return new TechnicalAdminView.ConfigItem(
                definitions.size(),
                providerItems.size(),
                providerItems
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
