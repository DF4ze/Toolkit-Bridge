package fr.ses10doigts.toolkitbridge.service.configuration.admin;

public final class StartupOrder {

    public static final int CONFIGURATION_BOOTSTRAP = 0;
    public static final int MEMORY_RUNTIME_CONFIGURATION = 1;
    public static final int LLM_PROVIDER_RUNTIME = 2;

    private StartupOrder() {
    }
}
