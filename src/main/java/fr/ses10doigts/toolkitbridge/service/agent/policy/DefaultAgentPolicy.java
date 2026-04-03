package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import org.springframework.stereotype.Component;

@Component
public class DefaultAgentPolicy implements AgentPolicy {

    public static final String NAME = "default";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean allowTools(AgentRuntime runtime, AgentRequest request) {
        return runtime.definition().toolsEnabled() && runtime.toolAccess().enabled();
    }
}
