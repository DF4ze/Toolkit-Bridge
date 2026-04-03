package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import org.springframework.stereotype.Component;

@Component
public class AgentOrchestratorResolver {

    private final AgentOrchestratorRegistry orchestratorRegistry;

    public AgentOrchestratorResolver(AgentOrchestratorRegistry orchestratorRegistry) {
        this.orchestratorRegistry = orchestratorRegistry;
    }

    public AgentOrchestrator resolve(AgentDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        AgentOrchestratorType type = definition.orchestratorType();
        if (type == null) {
            throw new IllegalArgumentException("definition orchestratorType must not be null");
        }
        return orchestratorRegistry.getByType(type);
    }
}
