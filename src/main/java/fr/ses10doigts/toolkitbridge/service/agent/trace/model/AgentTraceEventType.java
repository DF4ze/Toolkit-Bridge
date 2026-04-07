package fr.ses10doigts.toolkitbridge.service.agent.trace.model;

public enum AgentTraceEventType {
    TASK_STARTED,
    CONTEXT_ASSEMBLED,
    TOOL_CALL,
    DELEGATION,
    DEBATE,
    IMPROVEMENT_OBSERVATION,
    IMPROVEMENT_PROPOSAL,
    RESPONSE,
    ERROR
}
