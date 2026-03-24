package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentOrchestratorRegistry {

    private final List<AgentOrchestrator> orchestrators;

    public AgentOrchestratorRegistry(List<AgentOrchestrator> orchestrators) {
        this.orchestrators = orchestrators;
    }

    public AgentOrchestrator getByType(AgentOrchestratorType type) {
        return orchestrators.stream()
                .filter(orchestrator -> orchestrator.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown orchestrator type: " + type));
    }
}