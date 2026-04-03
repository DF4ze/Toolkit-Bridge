package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentOrchestratorRegistry {

    private final Map<AgentOrchestratorType, AgentOrchestrator> orchestratorsByType;

    public AgentOrchestratorRegistry(List<AgentOrchestrator> orchestrators) {
        this.orchestratorsByType = new EnumMap<>(AgentOrchestratorType.class);
        for (AgentOrchestrator orchestrator : orchestrators) {
            AgentOrchestratorType type = orchestrator.getType();
            AgentOrchestrator previous = orchestratorsByType.putIfAbsent(type, orchestrator);
            if (previous != null) {
                throw new IllegalStateException("Duplicate orchestrator registration for type: " + type);
            }
        }
    }

    public AgentOrchestrator getByType(AgentOrchestratorType type) {
        AgentOrchestrator orchestrator = orchestratorsByType.get(type);
        if (orchestrator == null) {
            throw new IllegalArgumentException("Unknown orchestrator type: " + type);
        }
        return orchestrator;
    }
}
