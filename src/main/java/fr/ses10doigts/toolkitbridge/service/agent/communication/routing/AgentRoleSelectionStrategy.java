package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

import java.util.List;
import java.util.Optional;

public interface AgentRoleSelectionStrategy {

    Optional<AgentRuntime> select(List<AgentRuntime> candidates);
}

