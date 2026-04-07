package fr.ses10doigts.toolkitbridge.persistence.model;

import java.util.Locale;

public enum PersistableObjectFamily {
    TASK,
    MESSAGE,
    TRACE,
    ARTIFACT,
    MEMORY,
    SCRIPTED_TOOL;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
