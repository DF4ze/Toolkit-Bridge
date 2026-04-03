package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;

import java.util.Optional;

public interface AgentRecipientResolver {

    Optional<ResolvedRecipient> resolve(AgentRecipient recipient);
}

