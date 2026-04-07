package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

import java.util.EnumSet;
import java.util.Set;

public interface AgentPolicy {

    String name();

    default ResolvedAgentPolicy resolve(AgentDefinition definition, AgentToolAccess toolAccess) {
        Set<String> allowedTools = definition != null && definition.toolsEnabled() && toolAccess != null && toolAccess.enabled()
                ? toolAccess.allowedTools()
                : Set.of();
        return new ResolvedAgentPolicy(
                name(),
                allowedTools,
                EnumSet.allOf(AgentMemoryScope.class),
                true,
                true,
                true,
                true
        );
    }

    default boolean allowTools(AgentRuntime runtime, AgentRequest request) {
        if (runtime == null) {
            return false;
        }
        return resolve(runtime.definition(), runtime.toolAccess()).allowsAnyTool()
                && runtime.definition().toolsEnabled()
                && runtime.toolAccess().enabled();
    }
}
