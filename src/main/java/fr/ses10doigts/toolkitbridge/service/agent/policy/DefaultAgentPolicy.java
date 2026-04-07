package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentPolicyDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class DefaultAgentPolicy implements AgentPolicy {

    public static final String NAME = "default";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ResolvedAgentPolicy resolve(AgentDefinition definition, AgentToolAccess toolAccess) {
        AgentPolicyDefinition configuredPolicy = definition == null || definition.policy() == null
                ? defaultPolicyDefinition()
                : definition.policy();

        Set<String> availableTools = toolAccess == null ? Set.of() : toolAccess.allowedTools();
        Set<String> allowedTools = configuredPolicy.allowedTools().isEmpty()
                ? availableTools
                : intersect(configuredPolicy.allowedTools(), availableTools);

        Set<AgentMemoryScope> memoryScopes = configuredPolicy.accessibleMemoryScopes().isEmpty()
                ? EnumSet.allOf(AgentMemoryScope.class)
                : configuredPolicy.accessibleMemoryScopes();

        return new ResolvedAgentPolicy(
                NAME,
                allowedTools,
                memoryScopes,
                configuredPolicy.delegationAllowed(),
                configuredPolicy.webAccessAllowed(),
                configuredPolicy.sharedWorkspaceWriteAllowed(),
                configuredPolicy.scriptedToolExecutionAllowed()
        );
    }

    private AgentPolicyDefinition defaultPolicyDefinition() {
        return new AgentPolicyDefinition(
                Set.of(),
                EnumSet.allOf(AgentMemoryScope.class),
                true,
                true,
                true,
                true
        );
    }

    private Set<String> intersect(Set<String> configuredTools, Set<String> availableTools) {
        LinkedHashSet<String> intersection = new LinkedHashSet<>();
        for (String configuredTool : configuredTools) {
            if (availableTools.contains(configuredTool)) {
                intersection.add(configuredTool);
            }
        }
        return Set.copyOf(intersection);
    }
}
