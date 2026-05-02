package fr.ses10doigts.toolkitbridge.controler.telegram.workflow;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.TelegramController;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramOrchestrationService;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramRoadmapService;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramSummaryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowTelegramControllerTest {

    @Test
    void workflowCommandReturnsExpectedMessage() {
        WorkflowTelegramOrchestrationService orchestrationService = mock(WorkflowTelegramOrchestrationService.class);
        when(orchestrationService.workflowHome(100L)).thenReturn("Workflow assistant");
        WorkflowTelegramController controller = new WorkflowTelegramController(
                mock(WorkflowTelegramRoadmapService.class),
                orchestrationService,
                mock(WorkflowTelegramSummaryService.class)
        );

        TelegramUpdateContext context = new TelegramUpdateContext(
                "Cortex",
                null,
                null,
                100L,
                200L,
                "/workflow",
                null,
                null,
                java.util.List.of(),
                false,
                null
        );

        String response = controller.workflow(context);

        assertThat(response).isEqualTo("Workflow assistant");
    }

    @Test
    void controllerIsDedicatedToCortexBot() {
        WorkflowTelegramController controller = new WorkflowTelegramController(
                mock(WorkflowTelegramRoadmapService.class),
                mock(WorkflowTelegramOrchestrationService.class),
                mock(WorkflowTelegramSummaryService.class)
        );

        TelegramController annot = controller.getClass().getAnnotation(TelegramController.class);

        assertThat(annot).isNotNull();
        assertThat(annot.bot()).isEqualTo("Cortex");
    }

    @Test
    void controllerDependsOnlyOnTelegramWorkflowServices() {
        WorkflowTelegramController controller = new WorkflowTelegramController(
                mock(WorkflowTelegramRoadmapService.class),
                mock(WorkflowTelegramOrchestrationService.class),
                mock(WorkflowTelegramSummaryService.class)
        );

        assertThat(controller.getClass().getDeclaredFields())
                .filteredOn(field -> !Modifier.isStatic(field.getModifiers()))
                .extracting(field -> field.getType().getName())
                .containsExactly(
                        WorkflowTelegramRoadmapService.class.getName(),
                        WorkflowTelegramOrchestrationService.class.getName(),
                        WorkflowTelegramSummaryService.class.getName()
                );
    }
}
