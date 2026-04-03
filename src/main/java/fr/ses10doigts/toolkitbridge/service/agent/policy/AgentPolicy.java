package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

public interface AgentPolicy {

    String name();

    boolean allowTools(AgentRuntime runtime, AgentRequest request);
}
