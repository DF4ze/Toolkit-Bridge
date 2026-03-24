package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;


import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;

public interface AgentOrchestrator {

    AgentOrchestratorType getType();

    AgentResponse handle(AgentDefinition agentDefinition, AgentRequest request);
}