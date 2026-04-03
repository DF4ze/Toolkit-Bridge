package fr.ses10doigts.toolkitbridge.service.agent.artifact.config;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "toolkit.artifacts")
@Data
public class ArtifactStorageProperties {

    private boolean enabled = true;
    private String contentFolder = "artifacts";
    private Retention retention = new Retention();

    @Data
    public static class Retention {
        private Duration defaultTtl = Duration.ofHours(24);
        private Map<String, Duration> byType = new HashMap<>(defaultByType());

        public Duration ttlFor(ArtifactType type) {
            if (type == null) {
                return defaultTtl;
            }
            Duration configured = byType.get(normalize(type.key()));
            if (configured == null) {
                configured = byType.get(normalize(type.name()));
            }
            if (configured == null) {
                configured = byType.get(normalize(type.label()));
            }
            if (configured == null) {
                configured = byType.entrySet().stream()
                        .filter(entry -> normalize(entry.getKey()).equals(normalize(type.key())))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            return configured == null ? defaultTtl : configured;
        }

        public void setByType(Map<String, Duration> byType) {
            if (byType == null) {
                this.byType = new HashMap<>();
                return;
            }
            Map<String, Duration> normalized = new HashMap<>();
            byType.forEach((key, value) -> normalized.put(normalize(key), value));
            this.byType = normalized;
        }

        private static Map<String, Duration> defaultByType() {
            Map<String, Duration> defaults = new HashMap<>();
            defaults.put(ArtifactType.REPORT.key(), Duration.ofDays(7));
            defaults.put(ArtifactType.PATCH.key(), Duration.ofDays(7));
            defaults.put(ArtifactType.SCRIPT.key(), Duration.ofDays(7));
            defaults.put(ArtifactType.PLAN.key(), Duration.ofDays(14));
            defaults.put(ArtifactType.SUMMARY.key(), Duration.ofDays(3));
            defaults.put(ArtifactType.FILE.key(), Duration.ofDays(30));
            defaults.put(ArtifactType.MEMORY_CANDIDATE.key(), Duration.ofDays(30));
            return defaults;
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            return value.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
        }
    }
}
