package fr.ses10doigts.toolkitbridge.service.agent.debate.service;

import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessagePayload;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateContext;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateMessageCommand;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateStage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DebateMessageFactory {

    static final String DEBATE_ID_KEY = "debateId";
    static final String DEBATE_STAGE_KEY = "debateStage";
    static final String DEBATE_ROOT_MESSAGE_ID_KEY = "debateRootMessageId";
    static final String DEBATE_PARENT_MESSAGE_ID_KEY = "debateParentMessageId";
    static final String DEBATE_TASK_ID_KEY = "debateTaskId";
    static final String DEBATE_SUBJECT_ARTIFACT_ID_KEY = "debateSubjectArtifactId";
    static final String DEBATE_INITIATOR_AGENT_ID_KEY = "debateInitiatorAgentId";

    public AgentMessage create(DebateMessageCommand command) {
        DebateContext debateContext = command.debateContext();
        AgentMessage message = AgentMessage.create(
                debateContext.debateId(),
                command.senderAgentId(),
                command.recipient(),
                debateContext.stage().toMessageType(),
                new AgentMessagePayload(
                        command.text(),
                        command.channelType(),
                        command.channelUserId(),
                        command.channelConversationId(),
                        command.projectId(),
                        mergeContext(command.context(), debateContext),
                        command.artifacts()
                )
        );

        if (debateContext.rootMessageId() == null && debateContext.stage() == DebateStage.QUESTION) {
            DebateContext normalized = debateContext.withRootMessage(message.messageId());
            return new AgentMessage(
                    message.messageId(),
                    message.correlationId(),
                    message.senderAgentId(),
                    message.recipient(),
                    message.timestamp(),
                    message.type(),
                    new AgentMessagePayload(
                            message.payload().text(),
                            message.payload().channelType(),
                            message.payload().channelUserId(),
                            message.payload().channelConversationId(),
                            message.payload().projectId(),
                            mergeContext(command.context(), normalized),
                            message.payload().artifacts()
                    )
            );
        }

        return message;
    }

    public Optional<DebateContext> extract(AgentMessage message) {
        if (message == null || message.payload() == null) {
            return Optional.empty();
        }
        Map<String, Object> context = message.payload().context();
        Object debateId = context.get(DEBATE_ID_KEY);
        Object stage = context.get(DEBATE_STAGE_KEY);
        Object taskId = context.get(DEBATE_TASK_ID_KEY);
        Object subjectArtifactId = context.get(DEBATE_SUBJECT_ARTIFACT_ID_KEY);
        Object initiatorAgentId = context.get(DEBATE_INITIATOR_AGENT_ID_KEY);

        if (debateId == null || stage == null || taskId == null || subjectArtifactId == null || initiatorAgentId == null) {
            return Optional.empty();
        }

        return Optional.of(new DebateContext(
                String.valueOf(debateId),
                String.valueOf(taskId),
                DebateStage.valueOf(String.valueOf(stage)),
                valueOrNull(context.get(DEBATE_ROOT_MESSAGE_ID_KEY)),
                valueOrNull(context.get(DEBATE_PARENT_MESSAGE_ID_KEY)),
                String.valueOf(subjectArtifactId),
                String.valueOf(initiatorAgentId)
        ));
    }

    private Map<String, Object> mergeContext(Map<String, Object> sourceContext, DebateContext debateContext) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (sourceContext != null) {
            merged.putAll(sourceContext);
        }
        merged.put(DEBATE_ID_KEY, debateContext.debateId());
        merged.put(DEBATE_STAGE_KEY, debateContext.stage().name());
        merged.put(DEBATE_TASK_ID_KEY, debateContext.taskId());
        merged.put(DEBATE_SUBJECT_ARTIFACT_ID_KEY, debateContext.subjectArtifactId());
        merged.put(DEBATE_INITIATOR_AGENT_ID_KEY, debateContext.initiatorAgentId());
        if (debateContext.rootMessageId() != null) {
            merged.put(DEBATE_ROOT_MESSAGE_ID_KEY, debateContext.rootMessageId());
        }
        if (debateContext.parentMessageId() != null) {
            merged.put(DEBATE_PARENT_MESSAGE_ID_KEY, debateContext.parentMessageId());
        }
        return Map.copyOf(merged);
    }

    private String valueOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
