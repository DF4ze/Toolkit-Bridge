package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;


import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

public interface AgentOrchestrator {

    AgentOrchestratorType getType();

    AgentResponse handle(AgentRuntime runtime, AgentRequest request);
}
