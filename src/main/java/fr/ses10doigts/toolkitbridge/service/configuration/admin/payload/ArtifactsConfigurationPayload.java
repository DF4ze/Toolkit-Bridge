package fr.ses10doigts.toolkitbridge.service.configuration.admin.payload;

import lombok.Data;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ArtifactsConfigurationPayload {

    private Boolean enabled;
    private String contentFolder;
    private Retention retention = new Retention();

    @Data
    public static class Retention {
        private Duration defaultTtl;
        private Map<String, Duration> byType = new LinkedHashMap<>();
    }
}
