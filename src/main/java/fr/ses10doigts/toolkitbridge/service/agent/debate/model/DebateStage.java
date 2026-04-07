package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessageType;

public enum DebateStage {
    QUESTION,
    RESPONSE,
    CRITIQUE,
    SYNTHESIS;

    public AgentMessageType toMessageType() {
        return switch (this) {
            case QUESTION -> AgentMessageType.QUESTION;
            case RESPONSE -> AgentMessageType.ANSWER;
            case CRITIQUE -> AgentMessageType.CRITIQUE;
            case SYNTHESIS -> AgentMessageType.SYNTHESIS;
        };
    }

    public static DebateStage fromMessageType(AgentMessageType messageType) {
        if (messageType == null) {
            throw new IllegalArgumentException("messageType must not be null");
        }
        return switch (messageType) {
            case QUESTION -> QUESTION;
            case ANSWER -> RESPONSE;
            case CRITIQUE -> CRITIQUE;
            case SYNTHESIS -> SYNTHESIS;
            default -> throw new IllegalArgumentException("Unsupported debate message type: " + messageType);
        };
    }
}
