package fr.ses10doigts.toolkitbridge.service.agent.definition;

import fr.ses10doigts.toolkitbridge.config.agent.AgentsProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentDefinitionService {

    private final List<AgentDefinition> definitions;

    public AgentDefinitionService(AgentsProperties properties) {
        this.definitions = properties.getDefinitions()
                .stream()
                .map(AgentDefinition::fromProperties)
                .toList();
    }

    public Optional<AgentDefinition> findById(String agentId) {
        return definitions.stream()
                .filter(agent -> agent.id().equals(agentId))
                .findFirst();
    }

    public Optional<AgentDefinition> findByTelegramBotId(String telegramBotId) {
        return definitions.stream()
                .filter(agent -> agent.telegramBotId().equals(telegramBotId))
                .findFirst();
    }

    public List<AgentDefinition> findAll() {
        return definitions;
    }

}