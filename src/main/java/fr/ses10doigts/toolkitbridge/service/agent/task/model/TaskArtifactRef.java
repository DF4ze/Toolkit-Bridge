package fr.ses10doigts.toolkitbridge.service.agent.task.model;

public record TaskArtifactRef(
        String artifactId,
        String artifactType,
        String location
) {
    public TaskArtifactRef {
        if (isBlank(artifactId)) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
