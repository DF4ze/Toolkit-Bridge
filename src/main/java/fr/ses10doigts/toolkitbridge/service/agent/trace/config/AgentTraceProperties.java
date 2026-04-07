package fr.ses10doigts.toolkitbridge.service.agent.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.observability.agent-tracing")
public class AgentTraceProperties {

    private boolean enabled = true;
    private final File file = new File();
    private final Memory memory = new Memory();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public File getFile() {
        return file;
    }

    public Memory getMemory() {
        return memory;
    }

    public static class File {
        private boolean enabled = true;
        private String rootPath = "workspace/observability/agent-traces";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRootPath() {
            return rootPath;
        }

        public void setRootPath(String rootPath) {
            this.rootPath = rootPath;
        }
    }

    public static class Memory {
        private boolean enabled = true;
        private int maxEvents = 1_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxEvents() {
            return maxEvents;
        }

        public void setMaxEvents(int maxEvents) {
            this.maxEvents = maxEvents;
        }
    }
}
