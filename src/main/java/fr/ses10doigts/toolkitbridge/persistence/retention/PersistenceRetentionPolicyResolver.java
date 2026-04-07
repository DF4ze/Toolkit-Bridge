package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Component
public class PersistenceRetentionPolicyResolver {

    private final PersistenceRetentionProperties properties;

    public PersistenceRetentionPolicyResolver(PersistenceRetentionProperties properties) {
        this.properties = properties;
    }

    public RetentionPolicy resolve(DurableObject durableObject) {
        if (durableObject == null) {
            throw new IllegalArgumentException("durableObject must not be null");
        }
        return resolve(durableObject.persistableFamily(), durableObject.persistenceDomain());
    }

    public RetentionPolicy resolve(PersistableObjectFamily family) {
        return resolve(family, null);
    }

    public RetentionPolicy resolve(PersistableObjectFamily family, String domain) {
        if (family == null) {
            throw new IllegalArgumentException("family must not be null");
        }

        PersistenceRetentionProperties.FamilyPolicy familyPolicy = properties.policyFor(family);
        PersistenceRetentionProperties.DomainPolicy domainPolicy = familyPolicy.domainPolicy(domain);

        Duration ttl = domainPolicy != null && domainPolicy.getTtl() != null
                ? domainPolicy.getTtl()
                : familyPolicy.getTtl() != null ? familyPolicy.getTtl() : properties.getDefaultTtl();
        RetentionDisposition disposition = domainPolicy != null && domainPolicy.getDisposition() != null
                ? domainPolicy.getDisposition()
                : familyPolicy.getDisposition() != null ? familyPolicy.getDisposition() : RetentionDisposition.PURGE;

        return new RetentionPolicy(family, normalize(domain), ttl, disposition);
    }

    public Instant computeExpiration(PersistableObjectFamily family, String domain, Instant createdAt) {
        Instant safeCreatedAt = createdAt == null ? Instant.now() : createdAt;
        return safeCreatedAt.plus(resolve(family, domain).ttl());
    }

    private String normalize(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return domain.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('.', '_');
    }
}
