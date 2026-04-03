package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipientKind;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultAgentRecipientResolver implements AgentRecipientResolver {

    private final AgentRuntimeDirectory runtimeDirectory;
    private final AgentRoleSelectionStrategy roleSelectionStrategy;

    @Override
    public Optional<ResolvedRecipient> resolve(AgentRecipient recipient) {
        if (recipient == null) {
            return Optional.empty();
        }

        if (recipient.kind() == AgentRecipientKind.AGENT) {
            return runtimeDirectory.findByAgentId(recipient.agentId())
                    .map(runtime -> new ResolvedRecipient(runtime.agentId(), runtime.role(), runtime));
        }

        if (recipient.kind() == AgentRecipientKind.ROLE) {
            AgentRole role = recipient.role();
            List<AgentRuntime> candidates = runtimeDirectory.findByRole(role);
            return roleSelectionStrategy.select(candidates)
                    .map(runtime -> new ResolvedRecipient(runtime.agentId(), role, runtime));
        }

        return Optional.empty();
    }
}

