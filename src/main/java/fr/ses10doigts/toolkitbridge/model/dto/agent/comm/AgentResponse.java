package fr.ses10doigts.toolkitbridge.model.dto.agent.comm;

public record AgentResponse(
        String message,
        boolean error
) {

    public static AgentResponse success(String message) {
        return new AgentResponse(message, false);
    }

    public static AgentResponse error(String message) {
        return new AgentResponse(message, true);
    }
}