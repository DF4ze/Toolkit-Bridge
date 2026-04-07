package fr.ses10doigts.toolkitbridge.service.tool.scripted.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(
        name = "scripted_tool_metadata",
        indexes = {
                @Index(name = "idx_scripted_tool_name", columnList = "tool_name", unique = true),
                @Index(name = "idx_scripted_tool_activation", columnList = "activation_status"),
                @Index(name = "idx_scripted_tool_validation", columnList = "validation_status")
        }
)
public class ScriptedToolMetadata implements DurableObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "tool_name", nullable = false, length = 120, unique = true)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 5000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolCategory category = ToolCategory.INTERNAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tool_risk_level", nullable = false, length = 40)
    private ToolRiskLevel riskLevel = ToolRiskLevel.READ_ONLY;

    @Column(name = "parameters_schema_json", nullable = false, length = 20000)
    private String parametersSchemaJson = "{}";

    @Column(name = "capabilities_csv", nullable = false, length = 2000)
    private String capabilitiesCsv = "";

    @NotBlank
    @Column(name = "runtime_type", nullable = false, length = 40)
    private String runtimeType = "shell";

    @NotBlank
    @Column(name = "script_path", nullable = false, length = 500)
    private String scriptPath;

    @Column(name = "script_checksum", nullable = false, length = 128)
    private String scriptChecksum = "";

    @Column(nullable = false)
    private int version = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScriptedToolLifecycleState state = ScriptedToolLifecycleState.DRAFT;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "activation_status", nullable = false, length = 40)
    private ScriptedToolActivationStatus activationStatus = ScriptedToolActivationStatus.INACTIVE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 40)
    private ScriptedToolOriginType originType = ScriptedToolOriginType.AGENT_GENERATED;

    @Column(name = "origin_ref", length = 255)
    private String originRef;

    @Column(name = "created_by_agent_id", length = 120)
    private String createdByAgentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_class", nullable = false, length = 40)
    private ScriptedToolRiskClass riskClass = ScriptedToolRiskClass.READ_ONLY;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_mode", nullable = false, length = 40)
    private ScriptedToolValidationMode validationMode = ScriptedToolValidationMode.NONE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 40)
    private ScriptedToolValidationStatus validationStatus = ScriptedToolValidationStatus.NOT_REQUIRED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (parametersSchemaJson == null || parametersSchemaJson.isBlank()) {
            parametersSchemaJson = "{}";
        }
        if (capabilitiesCsv == null) {
            capabilitiesCsv = "";
        }
        if (scriptChecksum == null) {
            scriptChecksum = "";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.SCRIPTED_TOOL;
    }
}
