package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "toolkit.persistence.retention")
@Data
public class PersistenceRetentionProperties {

    private Duration defaultTtl = Duration.ofDays(30);
    private Map<String, FamilyPolicy> families = defaultFamilies();

    public FamilyPolicy policyFor(PersistableObjectFamily family) {
        if (family == null) {
            return new FamilyPolicy();
        }
        FamilyPolicy configured = families.get(normalize(family.key()));
        return configured == null ? new FamilyPolicy() : configured;
    }

    public void setFamilies(Map<String, FamilyPolicy> families) {
        Map<String, FamilyPolicy> normalized = defaultFamilies();
        if (families != null) {
            families.forEach((key, value) -> normalized.put(
                    normalize(key),
                    mergeFamilyPolicy(normalized.get(normalize(key)), value)
            ));
        }
        this.families = normalized;
    }

    @Data
    public static class FamilyPolicy {
        private Duration ttl;
        private RetentionDisposition disposition;
        private Map<String, DomainPolicy> domains = new LinkedHashMap<>();

        public DomainPolicy domainPolicy(String domain) {
            if (domain == null || domain.isBlank()) {
                return null;
            }
            return domains.get(normalize(domain));
        }

        public void setDomains(Map<String, DomainPolicy> domains) {
            Map<String, DomainPolicy> normalized = new LinkedHashMap<>();
            if (this.domains != null) {
                this.domains.forEach((key, value) -> normalized.put(normalize(key), copyDomainPolicy(value)));
            }
            if (domains != null) {
                domains.forEach((key, value) -> normalized.put(
                        normalize(key),
                        mergeDomainPolicy(normalized.get(normalize(key)), value)
                ));
            }
            this.domains = normalized;
        }
    }

    @Data
    public static class DomainPolicy {
        private Duration ttl;
        private RetentionDisposition disposition;
    }

    private static Map<String, FamilyPolicy> defaultFamilies() {
        LinkedHashMap<String, FamilyPolicy> defaults = new LinkedHashMap<>();
        defaults.put(PersistableObjectFamily.TASK.key(), familyPolicy(Duration.ofDays(30), RetentionDisposition.PRESERVE));
        defaults.put(PersistableObjectFamily.MESSAGE.key(), familyPolicy(Duration.ofDays(14), RetentionDisposition.PRESERVE));
        defaults.put(PersistableObjectFamily.TRACE.key(), familyPolicy(Duration.ofDays(7), RetentionDisposition.PURGE));
        defaults.put(PersistableObjectFamily.MEMORY.key(), familyPolicy(Duration.ofDays(180), RetentionDisposition.ARCHIVE));
        defaults.put(PersistableObjectFamily.SCRIPTED_TOOL.key(), familyPolicy(Duration.ofDays(180), RetentionDisposition.PRESERVE));

        FamilyPolicy artifactPolicy = familyPolicy(Duration.ofHours(24), RetentionDisposition.PURGE);
        artifactPolicy.getDomains().put("report", domainPolicy(Duration.ofDays(7), RetentionDisposition.PURGE));
        artifactPolicy.getDomains().put("patch", domainPolicy(Duration.ofDays(7), RetentionDisposition.PURGE));
        artifactPolicy.getDomains().put("script", domainPolicy(Duration.ofDays(7), RetentionDisposition.PURGE));
        artifactPolicy.getDomains().put("plan", domainPolicy(Duration.ofDays(14), RetentionDisposition.PRESERVE));
        artifactPolicy.getDomains().put("summary", domainPolicy(Duration.ofDays(3), RetentionDisposition.PURGE));
        artifactPolicy.getDomains().put("proposal", domainPolicy(Duration.ofDays(30), RetentionDisposition.ARCHIVE));
        artifactPolicy.getDomains().put("file", domainPolicy(Duration.ofDays(30), RetentionDisposition.PRESERVE));
        artifactPolicy.getDomains().put("memory_candidate", domainPolicy(Duration.ofDays(30), RetentionDisposition.ARCHIVE));
        defaults.put(PersistableObjectFamily.ARTIFACT.key(), artifactPolicy);

        return defaults;
    }

    private static FamilyPolicy familyPolicy(Duration ttl, RetentionDisposition disposition) {
        FamilyPolicy policy = new FamilyPolicy();
        policy.setTtl(ttl);
        policy.setDisposition(disposition);
        return policy;
    }

    private static DomainPolicy domainPolicy(Duration ttl, RetentionDisposition disposition) {
        DomainPolicy policy = new DomainPolicy();
        policy.setTtl(ttl);
        policy.setDisposition(disposition);
        return policy;
    }

    private static FamilyPolicy mergeFamilyPolicy(FamilyPolicy base, FamilyPolicy override) {
        if (base == null && override == null) {
            return new FamilyPolicy();
        }
        if (override == null) {
            return copyFamilyPolicy(base);
        }

        FamilyPolicy merged = copyFamilyPolicy(base);
        if (override.getTtl() != null) {
            merged.setTtl(override.getTtl());
        }
        if (override.getDisposition() != null) {
            merged.setDisposition(override.getDisposition());
        }
        merged.setDomains(override.getDomains());
        return merged;
    }

    private static FamilyPolicy copyFamilyPolicy(FamilyPolicy source) {
        FamilyPolicy copy = new FamilyPolicy();
        if (source == null) {
            return copy;
        }
        copy.setTtl(source.getTtl());
        copy.setDisposition(source.getDisposition());
        if (source.getDomains() != null) {
            LinkedHashMap<String, DomainPolicy> copiedDomains = new LinkedHashMap<>();
            source.getDomains().forEach((key, value) -> copiedDomains.put(normalize(key), copyDomainPolicy(value)));
            copy.setDomains(copiedDomains);
        }
        return copy;
    }

    private static DomainPolicy mergeDomainPolicy(DomainPolicy base, DomainPolicy override) {
        if (base == null && override == null) {
            return new DomainPolicy();
        }
        if (override == null) {
            return copyDomainPolicy(base);
        }

        DomainPolicy merged = copyDomainPolicy(base);
        if (override.getTtl() != null) {
            merged.setTtl(override.getTtl());
        }
        if (override.getDisposition() != null) {
            merged.setDisposition(override.getDisposition());
        }
        return merged;
    }

    private static DomainPolicy copyDomainPolicy(DomainPolicy source) {
        DomainPolicy copy = new DomainPolicy();
        if (source == null) {
            return copy;
        }
        copy.setTtl(source.getTtl());
        copy.setDisposition(source.getDisposition());
        return copy;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('.', '_');
    }
}
