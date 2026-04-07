package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolActivationStatus;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolDraft;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolLifecycleState;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolMetadata;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolOriginType;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolRiskClass;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolValidationMode;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolValidationStatus;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.StoredScriptedTool;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.repository.ScriptedToolMetadataRepository;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.store.ScriptedToolContentStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class ScriptedToolCatalogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ScriptedToolMetadataRepository repository;
    private final ScriptedToolContentStore contentStore;
    private final ObjectMapper objectMapper;

    public ScriptedToolCatalogService(
            ScriptedToolMetadataRepository repository,
            ScriptedToolContentStore contentStore,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.contentStore = contentStore;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StoredScriptedTool save(ScriptedToolDraft draft) {
        String normalizedName = normalizeName(draft.name());
        if (repository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("A scripted tool already exists with name=" + normalizedName);
        }

        int version = draft.version() == null || draft.version() < 1 ? 1 : draft.version();
        String runtimeType = defaultRuntimeType(draft.runtimeType());
        String scriptContent = draft.scriptContent() == null ? "" : draft.scriptContent();
        String relativeScriptPath = contentStore.save(normalizedName, version, runtimeType, scriptContent);

        ScriptedToolMetadata metadata = new ScriptedToolMetadata();
        metadata.setName(normalizedName);
        metadata.setDescription(defaultDescription(draft.description(), normalizedName));
        metadata.setCategory(draft.category() == null ? ToolCategory.INTERNAL : draft.category());
        metadata.setRiskLevel(draft.riskLevel() == null ? ToolRiskLevel.READ_ONLY : draft.riskLevel());
        metadata.setParametersSchemaJson(serializeSchema(draft.parametersSchema()));
        metadata.setCapabilitiesCsv(serializeCapabilities(draft.capabilities()));
        metadata.setRuntimeType(runtimeType);
        metadata.setScriptPath(relativeScriptPath);
        metadata.setScriptChecksum(computeChecksum(scriptContent));
        metadata.setVersion(version);
        metadata.setState(draft.state() == null ? ScriptedToolLifecycleState.DRAFT : draft.state());
        metadata.setActivationStatus(draft.activationStatus() == null
                ? ScriptedToolActivationStatus.INACTIVE
                : draft.activationStatus());
        metadata.setOriginType(draft.originType() == null ? ScriptedToolOriginType.AGENT_GENERATED : draft.originType());
        metadata.setOriginRef(blankToNull(draft.originRef()));
        metadata.setCreatedByAgentId(blankToNull(draft.createdByAgentId()));
        metadata.setRiskClass(draft.riskClass() == null ? ScriptedToolRiskClass.READ_ONLY : draft.riskClass());
        metadata.setValidationMode(draft.validationMode() == null
                ? ScriptedToolValidationMode.NONE
                : draft.validationMode());
        metadata.setValidationStatus(draft.validationStatus() == null
                ? ScriptedToolValidationStatus.NOT_REQUIRED
                : draft.validationStatus());

        try {
            return toStored(repository.save(metadata));
        } catch (RuntimeException exception) {
            contentStore.delete(relativeScriptPath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredScriptedTool> findByName(String toolName) {
        return repository.findByName(normalizeName(toolName)).map(this::toStored);
    }

    @Transactional(readOnly = true)
    public List<StoredScriptedTool> listStoredTools() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ScriptedToolMetadata::getName))
                .map(this::toStored)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoredScriptedTool> listExplicitlyActivatedTools() {
        return repository.findByActivationStatus(ScriptedToolActivationStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(ScriptedToolMetadata::getName))
                .map(this::toStored)
                .toList();
    }

    public String loadScriptContent(StoredScriptedTool tool) {
        return contentStore.load(tool.scriptPath());
    }

    private StoredScriptedTool toStored(ScriptedToolMetadata metadata) {
        return new StoredScriptedTool(
                metadata.getId(),
                metadata.getName(),
                metadata.getDescription(),
                metadata.getCategory(),
                deserializeSchema(metadata.getParametersSchemaJson()),
                deserializeCapabilities(metadata.getCapabilitiesCsv()),
                metadata.getRiskLevel(),
                metadata.getRuntimeType(),
                metadata.getVersion(),
                metadata.getState(),
                metadata.getActivationStatus(),
                metadata.getOriginType(),
                metadata.getOriginRef(),
                metadata.getCreatedByAgentId(),
                metadata.getRiskClass(),
                metadata.getValidationMode(),
                metadata.getValidationStatus(),
                metadata.getScriptPath(),
                metadata.getScriptChecksum(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt()
        );
    }

    private String serializeSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema == null ? Map.of() : schema);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize scripted tool parameter schema", e);
        }
    }

    private Map<String, Object> deserializeSchema(String schemaJson) {
        try {
            return objectMapper.readValue(
                    schemaJson == null || schemaJson.isBlank() ? "{}" : schemaJson,
                    MAP_TYPE
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize scripted tool parameter schema", e);
        }
    }

    private String serializeCapabilities(Set<ToolCapability> capabilities) {
        return (capabilities == null ? Set.<ToolCapability>of() : capabilities).stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private Set<ToolCapability> deserializeCapabilities(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(ToolCapability::valueOf)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String computeChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 checksum support is unavailable", e);
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Scripted tool name must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Scripted tool name must not be blank");
        }
        return normalized;
    }

    private String defaultDescription(String description, String normalizedName) {
        return description == null || description.isBlank()
                ? "Scripted tool " + normalizedName
                : description;
    }

    private String defaultRuntimeType(String runtimeType) {
        return runtimeType == null || runtimeType.isBlank()
                ? "shell"
                : runtimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
