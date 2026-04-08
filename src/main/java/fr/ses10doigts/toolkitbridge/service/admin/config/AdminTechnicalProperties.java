package fr.ses10doigts.toolkitbridge.service.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.admin.technical")
public class AdminTechnicalProperties {

    private int defaultListLimit = 50;
    private int maxListLimit = 200;
    private final Tasks tasks = new Tasks();

    public int getDefaultListLimit() {
        return defaultListLimit;
    }

    public void setDefaultListLimit(int defaultListLimit) {
        this.defaultListLimit = defaultListLimit;
    }

    public int getMaxListLimit() {
        return maxListLimit;
    }

    public void setMaxListLimit(int maxListLimit) {
        this.maxListLimit = maxListLimit;
    }

    public Tasks getTasks() {
        return tasks;
    }

    public int sanitizeLimit(Integer requestedLimit) {
        int fallback = Math.max(defaultListLimit, 1);
        if (requestedLimit == null) {
            return Math.min(fallback, Math.max(maxListLimit, 1));
        }
        int maxAllowed = Math.max(maxListLimit, 1);
        return Math.min(Math.max(requestedLimit, 1), maxAllowed);
    }

    public static class Tasks {
        private int maxEvents = 500;

        public int getMaxEvents() {
            return maxEvents;
        }

        public void setMaxEvents(int maxEvents) {
            this.maxEvents = maxEvents;
        }
    }
}
