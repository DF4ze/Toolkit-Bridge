package fr.ses10doigts.toolkitbridge.service.agent.process.store;

import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessHistoryEntry;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessUpdateRequest;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class WorkspaceExternalProcessStore implements ExternalProcessStore {

    private static final String CONTENT_FILE_NAME = "content.json";
    private static final String METADATA_FILE_NAME = "metadata.json";
    private static final String HISTORY_FILE_NAME = "history.jsonl";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC);

    private final WorkspaceService workspaceService;
    private final WorkspaceLayout workspaceLayout;
    private final ObjectMapper objectMapper;

    public WorkspaceExternalProcessStore(WorkspaceService workspaceService,
                                         WorkspaceLayout workspaceLayout,
                                         ObjectMapper objectMapper) {
        this.workspaceService = workspaceService;
        this.workspaceLayout = workspaceLayout;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ExternalProcessSnapshot> findById(String processId) {
        String normalizedId = normalizeProcessId(processId);
        try {
            Path root = workspaceService.getExternalProcessesRoot();
            ProcessPaths paths = processPaths(root, normalizedId);
            if (!Files.exists(paths.metadataPath()) || !Files.exists(paths.contentPath())) {
                return Optional.empty();
            }
            return Optional.of(readSnapshot(root, paths));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read external process " + normalizedId, e);
        }
    }

    @Override
    public ExternalProcessSnapshot save(ExternalProcessUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String normalizedId = normalizeProcessId(request.processId());
        String normalizedDescription = normalizeRequired(request.description(), "description");
        String normalizedMediaType = normalizeMediaType(request.mediaType());
        String normalizedContent = request.content() == null ? "" : request.content();
        String normalizedUpdatedBy = normalizeRequired(request.updatedBy(), "updatedBy");
        String normalizedChangeSummary = normalizeRequired(request.changeSummary(), "changeSummary");

        try {
            Path root = workspaceService.getExternalProcessesRoot();
            ProcessPaths paths = processPaths(root, normalizedId);
            Files.createDirectories(paths.processDirectory());

            StoredProcessMetadata existingMetadata = readMetadataIfPresent(paths.metadataPath());
            String backupPath = createBackupIfNeeded(root, paths);
            Instant now = Instant.now();
            String checksum = checksum(normalizedContent);

            Files.writeString(paths.contentPath(), normalizedContent, StandardCharsets.UTF_8);

            StoredProcessMetadata metadata = new StoredProcessMetadata(
                    normalizedId,
                    normalizedDescription,
                    normalizedMediaType,
                    checksum,
                    existingMetadata == null || existingMetadata.createdAt() == null ? now : existingMetadata.createdAt(),
                    now,
                    normalizedUpdatedBy,
                    normalizedChangeSummary,
                    workspaceLayout.relativize(root, paths.contentPath()),
                    workspaceLayout.relativize(root, paths.metadataPath())
            );
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(paths.metadataPath().toFile(), metadata);
            appendHistory(paths.historyPath(), new ExternalProcessHistoryEntry(
                    normalizedId,
                    now,
                    normalizedUpdatedBy,
                    normalizedChangeSummary,
                    checksum,
                    backupPath
            ));

            return readSnapshot(root, paths);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save external process " + normalizedId, e);
        }
    }

    @Override
    public List<ExternalProcessHistoryEntry> loadHistory(String processId) {
        String normalizedId = normalizeProcessId(processId);
        try {
            Path root = workspaceService.getExternalProcessesRoot();
            ProcessPaths paths = processPaths(root, normalizedId);
            if (!Files.exists(paths.historyPath())) {
                return List.of();
            }
            return Files.readAllLines(paths.historyPath(), StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(this::deserializeHistoryEntry)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read process history " + normalizedId, e);
        }
    }

    private ExternalProcessSnapshot readSnapshot(Path root, ProcessPaths paths) throws IOException {
        StoredProcessMetadata metadata = objectMapper.readValue(paths.metadataPath().toFile(), StoredProcessMetadata.class);
        String content = Files.readString(paths.contentPath(), StandardCharsets.UTF_8);
        String checksum = metadata.checksum() == null || metadata.checksum().isBlank()
                ? checksum(content)
                : metadata.checksum();
        return new ExternalProcessSnapshot(
                metadata.processId(),
                metadata.description(),
                metadata.mediaType(),
                content,
                checksum,
                metadata.relativeContentPath(),
                metadata.relativeMetadataPath(),
                metadata.createdAt(),
                metadata.updatedAt(),
                metadata.updatedBy(),
                metadata.changeSummary()
        );
    }

    private StoredProcessMetadata readMetadataIfPresent(Path metadataPath) throws IOException {
        if (!Files.exists(metadataPath)) {
            return null;
        }
        return objectMapper.readValue(metadataPath.toFile(), StoredProcessMetadata.class);
    }

    private String createBackupIfNeeded(Path root, ProcessPaths paths) throws IOException {
        if (!Files.exists(paths.contentPath()) && !Files.exists(paths.metadataPath())) {
            return null;
        }

        Path backupDirectory = paths.backupsDirectory().resolve(BACKUP_TIMESTAMP_FORMATTER.format(Instant.now())).normalize();
        Files.createDirectories(backupDirectory);
        if (Files.exists(paths.contentPath())) {
            Files.copy(paths.contentPath(), backupDirectory.resolve(CONTENT_FILE_NAME));
        }
        if (Files.exists(paths.metadataPath())) {
            Files.copy(paths.metadataPath(), backupDirectory.resolve(METADATA_FILE_NAME));
        }
        return workspaceLayout.relativize(root, backupDirectory);
    }

    private void appendHistory(Path historyPath, ExternalProcessHistoryEntry entry) throws IOException {
        Files.createDirectories(historyPath.getParent());
        String serialized = objectMapper.writeValueAsString(entry) + System.lineSeparator();
        Files.writeString(
                historyPath,
                serialized,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
        );
    }

    private ExternalProcessHistoryEntry deserializeHistoryEntry(String serialized) {
        try {
            return objectMapper.readValue(serialized, ExternalProcessHistoryEntry.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize external process history entry", e);
        }
    }

    private ProcessPaths processPaths(Path root, String normalizedId) {
        Path processDirectory = workspaceLayout.resolveWithinRoot(root, normalizedId, "external processes root");
        return new ProcessPaths(
                processDirectory,
                processDirectory.resolve(CONTENT_FILE_NAME).normalize(),
                processDirectory.resolve(METADATA_FILE_NAME).normalize(),
                processDirectory.resolve("backups").normalize(),
                processDirectory.resolve(HISTORY_FILE_NAME).normalize()
        );
    }

    private String normalizeProcessId(String processId) {
        String normalized = normalizeRequired(processId, "processId")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("processId must not be blank");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeMediaType(String mediaType) {
        return mediaType == null || mediaType.isBlank() ? "application/json" : mediaType.trim();
    }

    private String checksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 checksum support is unavailable", e);
        }
    }

    private record ProcessPaths(
            Path processDirectory,
            Path contentPath,
            Path metadataPath,
            Path backupsDirectory,
            Path historyPath
    ) {
    }

    private record StoredProcessMetadata(
            String processId,
            String description,
            String mediaType,
            String checksum,
            Instant createdAt,
            Instant updatedAt,
            String updatedBy,
            String changeSummary,
            String relativeContentPath,
            String relativeMetadataPath
    ) {
    }
}
