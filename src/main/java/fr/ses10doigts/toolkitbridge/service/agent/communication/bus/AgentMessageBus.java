package fr.ses10doigts.toolkitbridge.service.agent.communication.bus;

import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;

public interface AgentMessageBus {

    AgentMessageDispatchResult dispatch(AgentMessage message);
}

