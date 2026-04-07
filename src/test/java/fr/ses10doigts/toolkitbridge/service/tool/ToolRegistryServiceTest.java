package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryServiceTest {

    @Test
    void buildsDescriptorBackedCatalogAndFilteredDefinitions() {
        ToolRegistryService registry = new ToolRegistryService(List.of(
                new StubToolHandler(
                        "read_file",
                        ToolKind.NATIVE,
                        ToolCategory.FILES,
                        ToolRiskLevel.READ_ONLY,
                        Set.of(ToolCapability.FILE_READ)
                ),
                new StubToolHandler(
                        "run_command",
                        ToolKind.SCRIPTED,
                        ToolCategory.EXECUTION,
                        ToolRiskLevel.EXECUTION,
                        Set.of(ToolCapability.COMMAND_EXECUTION)
                )
        ));

        assertThat(registry.getToolNames()).containsExactlyInAnyOrder("read_file", "run_command");
        assertThat(registry.getDescriptor("run_command").scripted()).isTrue();
        assertThat(registry.getToolDescriptors())
                .extracting(ToolDescriptor::name)
                .containsExactly("read_file", "run_command");
        assertThat(registry.getToolDescriptors(Set.of("read_file")))
                .extracting(ToolDescriptor::category, ToolDescriptor::riskLevel)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(ToolCategory.FILES, ToolRiskLevel.READ_ONLY));
        assertThat(registry.getToolDefinitions(Set.of("run_command")))
                .extracting(definition -> definition.function().name())
                .containsExactly("run_command");
    }

    @Test
    void rejectsDescriptorNameMismatch() {
        assertThatThrownBy(() -> new ToolRegistryService(List.of(
                new MismatchedDescriptorToolHandler()
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tool descriptor name mismatch");
    }

    private static class StubToolHandler implements ToolHandler {
        private final String name;
        private final ToolKind kind;
        private final ToolCategory category;
        private final ToolRiskLevel riskLevel;
        private final Set<ToolCapability> capabilities;

        private StubToolHandler(
                String name,
                ToolKind kind,
                ToolCategory category,
                ToolRiskLevel riskLevel,
                Set<ToolCapability> capabilities
        ) {
            this.name = name;
            this.kind = kind;
            this.category = category;
            this.riskLevel = riskLevel;
            this.capabilities = capabilities;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ToolKind kind() {
            return kind;
        }

        @Override
        public ToolCategory category() {
            return category;
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return riskLevel;
        }

        @Override
        public Set<ToolCapability> capabilities() {
            return capabilities;
        }

        @Override
        public String description() {
            return name + " description";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object");
        }

        @Override
        public ToolExecutionResult execute(Map<String, Object> arguments) {
            return ToolExecutionResult.builder().error(false).build();
        }
    }

    private static final class MismatchedDescriptorToolHandler extends StubToolHandler {

        private MismatchedDescriptorToolHandler() {
            super(
                    "Read_File",
                    ToolKind.NATIVE,
                    ToolCategory.FILES,
                    ToolRiskLevel.READ_ONLY,
                    Set.of(ToolCapability.FILE_READ)
            );
        }

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor(
                    "another_name",
                    kind(),
                    category(),
                    description(),
                    parametersSchema(),
                    capabilities(),
                    riskLevel()
            );
        }
    }
}
