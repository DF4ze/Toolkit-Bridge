package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WorkflowProjectRegistryService {

    private static final int MAX_PROJECT_NAME_LENGTH = 120;
    private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final WorkflowProjectRepository repository;
    private final WorkflowProjectPathPolicy pathPolicy;

    @Autowired
    public WorkflowProjectRegistryService(WorkflowProjectRepository repository, WorkflowProjectPathPolicy pathPolicy) {
        this.repository = repository;
        this.pathPolicy = pathPolicy == null ? new WorkflowProjectPathPolicy() : pathPolicy;
    }

    public WorkflowProjectRegistrationResult registerOrUpdate(String projectName, String projectPath) {
        NormalizedName normalizedName = normalizeProjectName(projectName);
        if (!normalizedName.valid()) {
            log.warn("Workflow project registration rejected: reason={}", normalizedName.reason());
            return WorkflowProjectRegistrationResult.failed(normalizedName.reason());
        }

        NormalizedPath normalizedPath = normalizeAndValidateProjectPath(projectPath);
        if (!normalizedPath.valid()) {
            log.warn("Workflow project registration rejected: projectName={}, reason={}",
                    normalizedName.projectName(), normalizedPath.reason());
            return WorkflowProjectRegistrationResult.failed(normalizedPath.reason());
        }

        Optional<WorkflowProjectEntity> existing = repository.findByProjectKey(normalizedName.projectKey());
        WorkflowProjectEntity entity = existing.orElseGet(WorkflowProjectEntity::new);
        entity.setProjectKey(normalizedName.projectKey());
        entity.setProjectName(normalizedName.projectName());
        entity.setProjectPath(normalizedPath.normalizedAbsolutePath().toString());

        repository.save(entity);

        if (existing.isPresent()) {
            log.info("Workflow project updated: projectName={}, projectKey={}",
                    normalizedName.projectName(), normalizedName.projectKey());
            return WorkflowProjectRegistrationResult.updated(normalizedName.projectKey(), normalizedName.projectName(), normalizedPath.normalizedAbsolutePath());
        }
        log.info("Workflow project registered: projectName={}, projectKey={}",
                normalizedName.projectName(), normalizedName.projectKey());
        return WorkflowProjectRegistrationResult.created(normalizedName.projectKey(), normalizedName.projectName(), normalizedPath.normalizedAbsolutePath());
    }

    public WorkflowProjectLookupResult lookup(String projectName) {
        NormalizedName normalizedName = normalizeProjectName(projectName);
        if (!normalizedName.valid()) {
            log.warn("Workflow project lookup failed — invalid name: reason={}", normalizedName.reason());
            return WorkflowProjectLookupResult.notFound(normalizedName.reason());
        }

        Optional<WorkflowProjectEntity> entity = repository.findByProjectKey(normalizedName.projectKey());
        log.debug("Workflow project lookup: projectKey={}, found={}", normalizedName.projectKey(), entity.isPresent());
        if (entity.isEmpty()) {
            log.warn("Workflow project lookup: project not found: projectKey={}", normalizedName.projectKey());
        }
        return entity
                .map(e -> WorkflowProjectLookupResult.found(
                        e.getProjectKey(),
                        e.getProjectName(),
                        Path.of(e.getProjectPath()).toAbsolutePath().normalize()
                ))
                .orElseGet(() -> WorkflowProjectLookupResult.notFound("Project not found"));
    }

    NormalizedName normalizeProjectName(String projectName) {
        if (projectName == null) {
            return NormalizedName.invalid("projectName must not be null");
        }
        String trimmed = projectName.trim();
        if (trimmed.isBlank()) {
            return NormalizedName.invalid("projectName must not be blank");
        }
        if (trimmed.length() > MAX_PROJECT_NAME_LENGTH) {
            return NormalizedName.invalid("projectName must be <= " + MAX_PROJECT_NAME_LENGTH + " characters");
        }
        if (trimmed.contains(" ")) {
            return NormalizedName.invalid("projectName must not contain spaces");
        }
        if (!PROJECT_NAME_PATTERN.matcher(trimmed).matches()) {
            return NormalizedName.invalid("projectName contains invalid characters");
        }

        String key = trimmed.toLowerCase(Locale.ROOT);
        return NormalizedName.valid(key, trimmed);
    }

    NormalizedPath normalizeAndValidateProjectPath(String projectPath) {
        if (projectPath == null) {
            return NormalizedPath.invalid("projectPath must not be null");
        }
        String trimmed = projectPath.trim();
        if (trimmed.isBlank()) {
            return NormalizedPath.invalid("projectPath must not be blank");
        }

        final Path rawPath;
        try {
            rawPath = Path.of(trimmed);
        } catch (IllegalArgumentException e) {
            return NormalizedPath.invalid("projectPath is invalid");
        }

        Path normalizedAbsolute = rawPath.toAbsolutePath().normalize();
        Path userHome = Path.of(System.getProperty("user.home", "")).toAbsolutePath().normalize();
        PathOsFamily family = detectOsFamily();
        if (pathPolicy.isDangerous(normalizedAbsolute, family, userHome)) {
            return NormalizedPath.invalid("projectPath is not allowed");
        }

        if (!Files.exists(normalizedAbsolute)) {
            return NormalizedPath.invalid("projectPath does not exist");
        }
        if (!Files.isDirectory(normalizedAbsolute)) {
            return NormalizedPath.invalid("projectPath must be a directory");
        }
        if (!Files.isReadable(normalizedAbsolute)) {
            return NormalizedPath.invalid("projectPath must be readable");
        }
        if (!Files.isWritable(normalizedAbsolute)) {
            return NormalizedPath.invalid("projectPath must be writable");
        }

        return NormalizedPath.valid(normalizedAbsolute);
    }

    private PathOsFamily detectOsFamily() {
        String osName = System.getProperty("os.name", "");
        String lower = osName.toLowerCase(Locale.ROOT);
        if (lower.contains("win")) {
            return PathOsFamily.WINDOWS;
        }
        return PathOsFamily.UNIX;
    }

    record NormalizedName(boolean valid, String projectKey, String projectName, String reason) {
        static NormalizedName valid(String projectKey, String projectName) {
            return new NormalizedName(true, projectKey, projectName, null);
        }

        static NormalizedName invalid(String reason) {
            return new NormalizedName(false, null, null, reason);
        }
    }

    record NormalizedPath(boolean valid, Path normalizedAbsolutePath, String reason) {
        static NormalizedPath valid(Path normalizedAbsolutePath) {
            return new NormalizedPath(true, normalizedAbsolutePath, null);
        }

        static NormalizedPath invalid(String reason) {
            return new NormalizedPath(false, null, reason);
        }
    }
}
