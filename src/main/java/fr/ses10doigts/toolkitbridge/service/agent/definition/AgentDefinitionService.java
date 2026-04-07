package fr.ses10doigts.toolkitbridge.service.agent.definition;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentDefinitionService {

    private final AdministrableConfigurationGateway configurationGateway;

    public AgentDefinitionService(AdministrableConfigurationGateway configurationGateway) {
        this.configurationGateway = configurationGateway;
    }

    public Optional<AgentDefinition> findById(String agentId) {
        return loadDefinitions().stream()
                .filter(agent -> agent.id().equals(agentId))
                .findFirst();
    }

    public Optional<AgentDefinition> findByTelegramBotId(String telegramBotId) {
        return loadDefinitions().stream()
                .filter(agent -> agent.telegramBotId().equals(telegramBotId))
                .findFirst();
    }

    public List<AgentDefinition> findAll() {
        return loadDefinitions();
    }

    private List<AgentDefinition> loadDefinitions() {
        return configurationGateway.loadAgentDefinitions()
                .stream()
                .map(AgentDefinition::fromProperties)
                .toList();
    }

}
