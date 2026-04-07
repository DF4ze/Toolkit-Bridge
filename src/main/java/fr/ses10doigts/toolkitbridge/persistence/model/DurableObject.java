package fr.ses10doigts.toolkitbridge.persistence.model;

public interface DurableObject extends StorageClassifiedObject {

    PersistableObjectFamily persistableFamily();

    default String persistenceDomain() {
        return null;
    }

    @Override
    default PersistenceLifecycle persistenceLifecycle() {
        return PersistenceLifecycle.DURABLE;
    }
}
