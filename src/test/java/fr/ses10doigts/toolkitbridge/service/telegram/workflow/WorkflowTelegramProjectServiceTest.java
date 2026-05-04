package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistrationResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistryService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowTelegramProjectServiceTest {

    @Test
    void missingNameReturnsError() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("path=D:\\Repo"));

        assertThat(msg).contains("Action impossible");
        assertThat(msg).contains("Missing project name");
    }

    @Test
    void missingPathReturnsError() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge"));

        assertThat(msg).contains("Action impossible");
        assertThat(msg).contains("Missing project path");
    }

    @Test
    void parsesUnquotedPathFromSingleToken() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.registerOrUpdate("ToolkitBridge", "D:\\Documents\\Spring\\Toolkit-Bridge"))
                .thenReturn(WorkflowProjectRegistrationResult.created("toolkitbridge", "ToolkitBridge", Path.of("D:\\Documents\\Spring\\Toolkit-Bridge")));
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge", "path=D:\\Documents\\Spring\\Toolkit-Bridge"));

        assertThat(msg).contains("Action effectuee");
        assertThat(msg).contains("Project registered: ToolkitBridge");
        assertThat(msg).doesNotContain("D:\\");
        assertThat(msg).doesNotContain("C:\\");
        assertThat(msg).doesNotContain("/home/");
    }

    @Test
    void parsesDoubleQuotedPathWithSpaces() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.registerOrUpdate("ToolkitBridge", "D:\\Documents\\Spring\\Mon Projet"))
                .thenReturn(WorkflowProjectRegistrationResult.updated("toolkitbridge", "ToolkitBridge", Path.of("D:\\Documents\\Spring\\Mon Projet")));
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge", "path=\"D:\\Documents\\Spring\\Mon", "Projet\""));

        assertThat(msg).contains("Action effectuee");
        assertThat(msg).contains("Project updated: ToolkitBridge");
        assertThat(msg).doesNotContain("D:\\");
    }

    @Test
    void parsesSingleQuotedPathWithSpaces() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.registerOrUpdate("ToolkitBridge", "D:\\Documents\\Spring\\Mon Projet"))
                .thenReturn(WorkflowProjectRegistrationResult.created("toolkitbridge", "ToolkitBridge", Path.of("D:\\Documents\\Spring\\Mon Projet")));
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge", "path='D:\\Documents\\Spring\\Mon", "Projet'"));

        assertThat(msg).contains("Action effectuee");
        assertThat(msg).contains("Project registered: ToolkitBridge");
        assertThat(msg).doesNotContain("D:\\");
    }

    @Test
    void missingClosingQuoteReturnsError() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge", "path=\"D:\\Documents\\Spring\\Mon", "Projet"));

        assertThat(msg).contains("Action impossible");
        assertThat(msg).contains("missing closing quote");
    }

    @Test
    void registryFailureReturnsError() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.registerOrUpdate("ToolkitBridge", "D:\\Repo")).thenReturn(WorkflowProjectRegistrationResult.failed("projectPath does not exist"));
        WorkflowTelegramProjectService service = new WorkflowTelegramProjectService(registry);

        String msg = service.setProject(List.of("name=ToolkitBridge", "path=D:\\Repo"));

        assertThat(msg).contains("Action impossible");
        assertThat(msg).contains("projectPath does not exist");
    }

    @Test
    void workflowHomeContainsProjectSetCommand() {
        WorkflowTelegramSession session = WorkflowTelegramSession.idle(1L);

        String home = WorkflowTelegramMessageRenderer.workflowHome(session);

        assertThat(home).contains("/workflow_project_set");
    }
}

