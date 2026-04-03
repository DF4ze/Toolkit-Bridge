package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;


public final class AgentRuntimeState {

    private AgentRuntimeExecutionSnapshot snapshot;

    public AgentRuntimeState() {
        this.snapshot = AgentRuntimeExecutionSnapshot.idle();
    }

    public synchronized AgentRuntimeExecutionSnapshot snapshot() {
        return snapshot;
    }

    public synchronized void startExecution(
            String traceId,
            String channelType,
            String channelConversationId,
            String currentTask,
            String activeContext
    ) {
        this.snapshot = AgentRuntimeExecutionSnapshot.running(
                snapshot.availability(),
                traceId,
                channelType,
                channelConversationId,
                currentTask,
                activeContext
        );
    }

    public synchronized void finishExecution() {
        this.snapshot = snapshot.toIdle();
    }

    public synchronized void markUnavailable() {
        this.snapshot = snapshot.toAvailability(AgentAvailability.UNAVAILABLE);
    }

    public synchronized void markAvailable() {
        this.snapshot = snapshot.toAvailability(AgentAvailability.AVAILABLE);
    }

    public synchronized boolean isBusy() {
        return snapshot.busy();
    }
}
