package fr.ses10doigts.toolkitbridge.memory.semantic.model;

public enum MemoryScope {
    /**
     * Durable information tied to agent identity/behavior.
     */
    AGENT,
    /**
     * Durable information tied to a specific end user.
     */
    USER,
    /**
     * Durable information tied to a specific project/workspace.
     */
    PROJECT,
    /**
     * Durable global/system information shared by platform rules.
     */
    SYSTEM,
    /**
     * Legacy alias kept for backward compatibility with older persisted values.
     */
    @Deprecated
    SHARED
}
