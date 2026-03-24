package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import org.springframework.stereotype.Component;

@Component
public class ChatAgentOrchestrator implements AgentOrchestrator {

    @Override
    public AgentOrchestratorType getType() {
        return "chat";
    }

    @Override
    public AgentResponse handle(AgentDefinition agentDefinition, AgentRequest request) {
        return AgentResponse.success(
                "ChatAgentOrchestrator not yet connected to LLM for agent '%s'".formatted(agentDefinition.id())
        );
    }
}