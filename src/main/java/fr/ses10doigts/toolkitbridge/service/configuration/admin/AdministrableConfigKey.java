package fr.ses10doigts.toolkitbridge.service.configuration.admin;

public enum AdministrableConfigKey {

    AGENT_DEFINITIONS("agent.definitions"),
    OPENAI_LIKE_PROVIDERS("llm.openai_like.providers"),
    ARTIFACTS_CONFIGURATION("artifacts.configuration"),
    RETENTION_CONFIGURATION("retention.configuration"),
    MEMORY_CONFIGURATION("memory.configuration");

    private final String storageKey;

    AdministrableConfigKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String storageKey() {
        return storageKey;
    }
}

