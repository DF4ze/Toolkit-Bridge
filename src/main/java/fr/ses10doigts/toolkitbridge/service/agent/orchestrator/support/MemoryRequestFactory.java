package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import org.springframework.stereotype.Component;

@Component
public class MemoryRequestFactory {

    public MemoryContextRequest from(AgentDefinition definition,
                                     AgentRequest request,
                                     OrchestrationRequestContext context) {
        return new MemoryContextRequest(
                context.agentId(),
                request.channelUserId(),
                definition.telegramBotId(),
                context.projectId(),
                context.userMessage(),
                context.conversationId(),
                null,
                null,
                null,
                null
        );
    }
}
