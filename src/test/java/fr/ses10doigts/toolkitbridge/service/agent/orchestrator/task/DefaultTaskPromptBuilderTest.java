package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.process.ExternalProcessService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.PromptProcessTemplate;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTaskPromptBuilderTest {

    @Test
    void buildsTaskPromptFromExternalizedProcessTemplate() {
        ExternalProcessService externalProcessService = mock(ExternalProcessService.class);
        when(externalProcessService.loadTypedContent(
                DefaultTaskPromptBuilder.TASK_EXECUTION_PROCESS_ID,
                PromptProcessTemplate.class
        )).thenReturn(new PromptProcessTemplate(
                "{{baseSystemPrompt}}\nTASK MODE",
                "Objective={{taskObjective}}\nTrace={{traceId}}\nMemory={{memoryContextText}}"
        ));
        when(externalProcessService.renderTemplate(any(), any())).thenCallRealMethod();

        DefaultTaskPromptBuilder builder = new DefaultTaskPromptBuilder(externalProcessService);
        AgentRuntime runtime = new AgentRuntime(
                new AgentDefinition(
                        "agent-1",
                        "Agent 1",
                        null,
                        AgentRole.EXECUTOR,
                        AgentOrchestratorType.TASK,
                        "provider",
                        "model",
                        "Base prompt",
                        "default",
                        true
                ),
                null,
                null,
                null,
                (fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy) null,
                null,
                null
        );
        Task task = new Task(
                "task-1",
                "Prepare release note",
                "user",
                "agent-1",
                null,
                "trace-1",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                Map.of(),
                List.of()
        );

        TaskPrompt prompt = builder.build(runtime, null, null, task, new MemoryContext("Relevant memory", List.of()));

        assertThat(prompt.systemPrompt()).contains("Base prompt").contains("TASK MODE");
        assertThat(prompt.userPrompt()).contains("Prepare release note").contains("trace-1").contains("Relevant memory");
    }
}
