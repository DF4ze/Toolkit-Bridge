package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

public record ArtifactContentPointer(
        String storageKind,
        String location,
        String mediaType,
        long sizeBytes
) {

    public ArtifactContentPointer {
        if (storageKind == null || storageKind.isBlank()) {
            throw new IllegalArgumentException("storageKind must not be blank");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be >= 0");
        }
        mediaType = (mediaType == null || mediaType.isBlank()) ? "text/plain" : mediaType.trim();
    }
}
