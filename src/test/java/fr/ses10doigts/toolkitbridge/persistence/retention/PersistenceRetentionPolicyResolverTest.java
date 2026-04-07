package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRetentionPolicyResolverTest {

    @Test
    void resolvesDomainOverrideBeforeFamilyDefault() {
        PersistenceRetentionProperties properties = new PersistenceRetentionProperties();

        PersistenceRetentionProperties.FamilyPolicy artifactPolicy = new PersistenceRetentionProperties.FamilyPolicy();
        artifactPolicy.setTtl(Duration.ofHours(10));
        artifactPolicy.setDisposition(RetentionDisposition.PURGE);

        PersistenceRetentionProperties.DomainPolicy reportPolicy = new PersistenceRetentionProperties.DomainPolicy();
        reportPolicy.setTtl(Duration.ofHours(48));
        reportPolicy.setDisposition(RetentionDisposition.PRESERVE);
        artifactPolicy.setDomains(Map.of("report", reportPolicy));

        properties.setFamilies(Map.of("artifact", artifactPolicy));

        PersistenceRetentionPolicyResolver resolver = new PersistenceRetentionPolicyResolver(properties);

        RetentionPolicy report = resolver.resolve(PersistableObjectFamily.ARTIFACT, "report");
        RetentionPolicy script = resolver.resolve(PersistableObjectFamily.ARTIFACT, "script");
        RetentionPolicy custom = resolver.resolve(PersistableObjectFamily.ARTIFACT, "custom");

        assertThat(report.ttl()).isEqualTo(Duration.ofHours(48));
        assertThat(report.disposition()).isEqualTo(RetentionDisposition.PRESERVE);
        assertThat(script.ttl()).isEqualTo(Duration.ofDays(7));
        assertThat(script.disposition()).isEqualTo(RetentionDisposition.PURGE);
        assertThat(custom.ttl()).isEqualTo(Duration.ofHours(10));
        assertThat(custom.disposition()).isEqualTo(RetentionDisposition.PURGE);
    }

    @Test
    void keepsDefaultFamiliesAndDomainsWhenConfigurationOverridesOnlyPartially() {
        PersistenceRetentionProperties properties = new PersistenceRetentionProperties();

        PersistenceRetentionProperties.FamilyPolicy artifactOverride = new PersistenceRetentionProperties.FamilyPolicy();
        PersistenceRetentionProperties.DomainPolicy reportOverride = new PersistenceRetentionProperties.DomainPolicy();
        reportOverride.setTtl(Duration.ofDays(10));
        artifactOverride.setDomains(Map.of("report", reportOverride));

        properties.setFamilies(Map.of("artifact", artifactOverride));

        PersistenceRetentionPolicyResolver resolver = new PersistenceRetentionPolicyResolver(properties);

        RetentionPolicy report = resolver.resolve(PersistableObjectFamily.ARTIFACT, "report");
        RetentionPolicy patch = resolver.resolve(PersistableObjectFamily.ARTIFACT, "patch");
        RetentionPolicy task = resolver.resolve(PersistableObjectFamily.TASK);

        assertThat(report.ttl()).isEqualTo(Duration.ofDays(10));
        assertThat(report.disposition()).isEqualTo(RetentionDisposition.PURGE);
        assertThat(patch.ttl()).isEqualTo(Duration.ofDays(7));
        assertThat(task.ttl()).isEqualTo(Duration.ofDays(30));
        assertThat(task.disposition()).isEqualTo(RetentionDisposition.PRESERVE);
    }

    @Test
    void computesExpirationFromResolvedPolicy() {
        PersistenceRetentionProperties properties = new PersistenceRetentionProperties();
        PersistenceRetentionPolicyResolver resolver = new PersistenceRetentionPolicyResolver(properties);
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        Instant expiration = resolver.computeExpiration(PersistableObjectFamily.ARTIFACT, "report", createdAt);

        assertThat(expiration).isEqualTo(Instant.parse("2026-01-08T10:00:00Z"));
    }
}
