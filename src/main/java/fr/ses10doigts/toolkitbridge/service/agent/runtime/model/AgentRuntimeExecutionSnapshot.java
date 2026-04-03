package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import java.time.Instant;

public record AgentRuntimeExecutionSnapshot(
        AgentAvailability availability,
        boolean busy,
        String currentTask,
        String activeContext,
        String traceId,
        String channelType,
        String channelConversationId,
        Instant startedAt,
        Instant updatedAt
) {

    public static AgentRuntimeExecutionSnapshot idle() {
        Instant now = Instant.now();
        return new AgentRuntimeExecutionSnapshot(
                AgentAvailability.AVAILABLE,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                now
        );
    }

    public static AgentRuntimeExecutionSnapshot running(
            AgentAvailability availability,
            String traceId,
            String channelType,
            String channelConversationId,
            String currentTask,
            String activeContext
    ) {
        Instant now = Instant.now();
        return new AgentRuntimeExecutionSnapshot(
                availability == null ? AgentAvailability.AVAILABLE : availability,
                true,
                currentTask,
                activeContext,
                traceId,
                channelType,
                channelConversationId,
                now,
                now
        );
    }

    public AgentRuntimeExecutionSnapshot toIdle() {
        return new AgentRuntimeExecutionSnapshot(
                availability,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now()
        );
    }

    public AgentRuntimeExecutionSnapshot toAvailability(AgentAvailability availability) {
        return new AgentRuntimeExecutionSnapshot(
                availability == null ? this.availability : availability,
                busy,
                currentTask,
                activeContext,
                traceId,
                channelType,
                channelConversationId,
                startedAt,
                Instant.now()
        );
    }
}
