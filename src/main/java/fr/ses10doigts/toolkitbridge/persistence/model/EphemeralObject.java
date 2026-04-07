package fr.ses10doigts.toolkitbridge.persistence.model;

public interface EphemeralObject extends StorageClassifiedObject {

    @Override
    default PersistenceLifecycle persistenceLifecycle() {
        return PersistenceLifecycle.EPHEMERAL;
    }
}
