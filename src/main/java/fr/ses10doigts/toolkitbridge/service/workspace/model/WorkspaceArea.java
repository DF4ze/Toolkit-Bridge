package fr.ses10doigts.toolkitbridge.service.workspace.model;

public enum WorkspaceArea {
    AGENT_WORKSPACE("agent workspace", true, false),
    SHARED_WORKSPACE("shared workspace", true, false),
    GLOBAL_CONTEXT("global context", false, true);

    private final String externalName;
    private final boolean writableArea;
    private final boolean stableKnowledgeArea;

    WorkspaceArea(String externalName, boolean writableArea, boolean stableKnowledgeArea) {
        this.externalName = externalName;
        this.writableArea = writableArea;
        this.stableKnowledgeArea = stableKnowledgeArea;
    }

    public String getExternalName() {
        return externalName;
    }

    public boolean isWritableArea() {
        return writableArea;
    }

    public boolean isStableKnowledgeArea() {
        return stableKnowledgeArea;
    }
}
