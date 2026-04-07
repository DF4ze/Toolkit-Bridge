package fr.ses10doigts.toolkitbridge.service.agent.process;

import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class DefaultExternalProcessCatalog {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public DefaultExternalProcessCatalog(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public Optional<ExternalProcessSnapshot> findById(String processId) {
        Resource metadataResource = resourceLoader.getResource(resourcePath(processId, "metadata.json"));
        Resource contentResource = resourceLoader.getResource(resourcePath(processId, "content.json"));
        if (!metadataResource.exists() || !contentResource.exists()) {
            return Optional.empty();
        }

        try (InputStream metadataStream = metadataResource.getInputStream();
             InputStream contentStream = contentResource.getInputStream()) {
            BundledProcessMetadata metadata = objectMapper.readValue(metadataStream, BundledProcessMetadata.class);
            String content = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
            return Optional.of(new ExternalProcessSnapshot(
                    metadata.processId(),
                    metadata.description(),
                    metadata.mediaType(),
                    content,
                    checksum(content),
                    metadata.relativeContentPath(),
                    metadata.relativeMetadataPath(),
                    metadata.createdAt(),
                    metadata.updatedAt(),
                    metadata.updatedBy(),
                    metadata.changeSummary()
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load bundled external process: " + processId, e);
        }
    }

    private String resourcePath(String processId, String fileName) {
        return "classpath:external-processes/" + processId + "/" + fileName;
    }

    private String checksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 checksum support is unavailable", e);
        }
    }

    private record BundledProcessMetadata(
            String processId,
            String description,
            String mediaType,
            Instant createdAt,
            Instant updatedAt,
            String updatedBy,
            String changeSummary,
            String relativeContentPath,
            String relativeMetadataPath
    ) {
    }
}
