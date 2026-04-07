package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;

import java.time.Duration;

public record RetentionPolicy(
        PersistableObjectFamily family,
        String domain,
        Duration ttl,
        RetentionDisposition disposition
) {
}
