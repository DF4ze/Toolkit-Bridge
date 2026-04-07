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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.databind.ObjectMapper;

class ScriptedToolCatalogServiceTest {

    @Test
    void savePersistsMetadataSeparatelyFromContentAndDefaultsToExplicitInactivity() {
        ScriptedToolMetadataRepository repository = mock(ScriptedToolMetadataRepository.class);
        ScriptedToolContentStore contentStore = mock(ScriptedToolContentStore.class);
        ScriptedToolCatalogService service = new ScriptedToolCatalogService(repository, contentStore, new ObjectMapper());

        when(repository.findByName("report_builder")).thenReturn(Optional.empty());
        when(contentStore.save("report_builder", 2, "python", "print('ok')"))
                .thenReturn("report_builder/v2/tool.python");
        when(repository.save(any(ScriptedToolMetadata.class))).thenAnswer(invocation -> {
            ScriptedToolMetadata saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        StoredScriptedTool stored = service.save(new ScriptedToolDraft(
                "Report_Builder",
                "Build a report",
                ToolCategory.EXECUTION,
                Map.of("type", "object"),
                Set.of(ToolCapability.COMMAND_EXECUTION),
                ToolRiskLevel.EXECUTION,
                "python",
                "print('ok')",
                2,
                ScriptedToolLifecycleState.READY,
                null,
                ScriptedToolOriginType.AGENT_GENERATED,
                "conversation-1",
                "agent-7",
                ScriptedToolRiskClass.WRITE,
                ScriptedToolValidationMode.HUMAN_REVIEW,
                ScriptedToolValidationStatus.DRAFT
        ));

        assertThat(stored.id()).isEqualTo(42L);
        assertThat(stored.name()).isEqualTo("report_builder");
        assertThat(stored.activationStatus()).isEqualTo(ScriptedToolActivationStatus.INACTIVE);
        assertThat(stored.parametersSchema()).containsEntry("type", "object");
        assertThat(stored.capabilities()).containsExactly(ToolCapability.COMMAND_EXECUTION);
        assertThat(stored.scriptPath()).isEqualTo("report_builder/v2/tool.python");
        verify(contentStore).save("report_builder", 2, "python", "print('ok')");
    }

    @Test
    void listExplicitlyActivatedToolsReturnsOnlyActiveOnes() {
        ScriptedToolMetadataRepository repository = mock(ScriptedToolMetadataRepository.class);
        ScriptedToolContentStore contentStore = mock(ScriptedToolContentStore.class);
        ScriptedToolCatalogService service = new ScriptedToolCatalogService(repository, contentStore, new ObjectMapper());

        ScriptedToolMetadata active = metadata("alpha", ScriptedToolActivationStatus.ACTIVE);
        ScriptedToolMetadata inactive = metadata("beta", ScriptedToolActivationStatus.INACTIVE);

        when(repository.findByActivationStatus(ScriptedToolActivationStatus.ACTIVE)).thenReturn(List.of(active));
        when(repository.findAll()).thenReturn(List.of(active, inactive));

        assertThat(service.listExplicitlyActivatedTools())
                .extracting(StoredScriptedTool::name)
                .containsExactly("alpha");
    }

    @Test
    void saveRejectsDuplicateNames() {
        ScriptedToolMetadataRepository repository = mock(ScriptedToolMetadataRepository.class);
        ScriptedToolContentStore contentStore = mock(ScriptedToolContentStore.class);
        ScriptedToolCatalogService service = new ScriptedToolCatalogService(repository, contentStore, new ObjectMapper());

        when(repository.findByName("duplicate")).thenReturn(Optional.of(new ScriptedToolMetadata()));

        assertThatThrownBy(() -> service.save(new ScriptedToolDraft(
                "duplicate",
                "desc",
                ToolCategory.INTERNAL,
                Map.of(),
                Set.of(),
                ToolRiskLevel.READ_ONLY,
                "shell",
                "echo hi",
                1,
                ScriptedToolLifecycleState.DRAFT,
                ScriptedToolActivationStatus.INACTIVE,
                ScriptedToolOriginType.AGENT_GENERATED,
                null,
                null,
                ScriptedToolRiskClass.READ_ONLY,
                ScriptedToolValidationMode.NONE,
                ScriptedToolValidationStatus.NOT_REQUIRED
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(contentStore, never()).save("duplicate", 1, "shell", "echo hi");
    }

    @Test
    void saveDeletesPersistedScriptWhenMetadataPersistenceFails() {
        ScriptedToolMetadataRepository repository = mock(ScriptedToolMetadataRepository.class);
        ScriptedToolContentStore contentStore = mock(ScriptedToolContentStore.class);
        ScriptedToolCatalogService service = new ScriptedToolCatalogService(repository, contentStore, new ObjectMapper());

        when(repository.findByName("cleanup_tool")).thenReturn(Optional.empty());
        when(contentStore.save("cleanup_tool", 1, "shell", "echo cleanup"))
                .thenReturn("cleanup_tool/v1/tool.shell");
        when(repository.save(any(ScriptedToolMetadata.class))).thenThrow(new IllegalStateException("db failure"));

        assertThatThrownBy(() -> service.save(new ScriptedToolDraft(
                "cleanup_tool",
                "desc",
                ToolCategory.INTERNAL,
                Map.of(),
                Set.of(),
                ToolRiskLevel.READ_ONLY,
                "shell",
                "echo cleanup",
                1,
                ScriptedToolLifecycleState.DRAFT,
                ScriptedToolActivationStatus.INACTIVE,
                ScriptedToolOriginType.AGENT_GENERATED,
                null,
                null,
                ScriptedToolRiskClass.READ_ONLY,
                ScriptedToolValidationMode.NONE,
                ScriptedToolValidationStatus.NOT_REQUIRED
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db failure");

        verify(contentStore).delete("cleanup_tool/v1/tool.shell");
    }

    private ScriptedToolMetadata metadata(String name, ScriptedToolActivationStatus activationStatus) {
        ScriptedToolMetadata metadata = new ScriptedToolMetadata();
        metadata.setId((long) name.length());
        metadata.setName(name);
        metadata.setDescription(name + " desc");
        metadata.setCategory(ToolCategory.INTERNAL);
        metadata.setRiskLevel(ToolRiskLevel.READ_ONLY);
        metadata.setParametersSchemaJson("{}");
        metadata.setCapabilitiesCsv("");
        metadata.setRuntimeType("shell");
        metadata.setScriptPath(name + "/v1/tool.shell");
        metadata.setVersion(1);
        metadata.setState(ScriptedToolLifecycleState.DRAFT);
        metadata.setActivationStatus(activationStatus);
        metadata.setOriginType(ScriptedToolOriginType.AGENT_GENERATED);
        metadata.setRiskClass(ScriptedToolRiskClass.READ_ONLY);
        metadata.setValidationMode(ScriptedToolValidationMode.NONE);
        metadata.setValidationStatus(ScriptedToolValidationStatus.NOT_REQUIRED);
        return metadata;
    }
}
