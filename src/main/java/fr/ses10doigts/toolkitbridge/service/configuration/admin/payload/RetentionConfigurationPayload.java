package fr.ses10doigts.toolkitbridge.service.configuration.admin.payload;

import fr.ses10doigts.toolkitbridge.persistence.retention.RetentionDisposition;
import lombok.Data;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class RetentionConfigurationPayload {

    private Duration defaultTtl;
    private Map<String, FamilyPolicyPayload> families = new LinkedHashMap<>();

    @Data
    public static class FamilyPolicyPayload {
        private Duration ttl;
        private RetentionDisposition disposition;
        private Map<String, DomainPolicyPayload> domains = new LinkedHashMap<>();
    }

    @Data
    public static class DomainPolicyPayload {
        private Duration ttl;
        private RetentionDisposition disposition;
    }
}
