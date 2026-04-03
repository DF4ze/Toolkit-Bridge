package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

import java.util.List;
import java.util.Optional;

public interface AgentRuntimeDirectory {

    Optional<AgentRuntime> findByAgentId(String agentId);

    List<AgentRuntime> findByRole(AgentRole role);
}

