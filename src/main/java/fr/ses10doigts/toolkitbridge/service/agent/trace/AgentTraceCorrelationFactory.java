package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentTraceCorrelationFactory {

    public AgentTraceCorrelation fromOrchestration(OrchestrationRequestContext context,
                                                   AgentRequest request,
                                                   Task task) {
        return new AgentTraceCorrelation(
                context == null ? null : context.traceId(),
                task == null ? null : task.taskId(),
                context == null ? null : context.agentId(),
                request == null ? null : extractMessageId(request.context())
        );
    }

    public AgentTraceCorrelation fromMessage(AgentMessage message, String resolvedAgentId) {
        return new AgentTraceCorrelation(
                message == null ? null : message.correlationId(),
                null,
                resolvedAgentId,
                message == null ? null : message.messageId()
        );
    }

    private String extractMessageId(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        Object raw = context.get("messageId");
        if (raw == null) {
            return null;
        }
        String messageId = String.valueOf(raw).trim();
        return messageId.isEmpty() ? null : messageId;
    }
}
