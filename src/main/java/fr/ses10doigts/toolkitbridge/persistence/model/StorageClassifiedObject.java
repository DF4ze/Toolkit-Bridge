package fr.ses10doigts.toolkitbridge.persistence.model;

public interface StorageClassifiedObject {

    PersistenceLifecycle persistenceLifecycle();

    default boolean isDurable() {
        return persistenceLifecycle() == PersistenceLifecycle.DURABLE;
    }

    default boolean isEphemeral() {
        return persistenceLifecycle() == PersistenceLifecycle.EPHEMERAL;
    }
}
