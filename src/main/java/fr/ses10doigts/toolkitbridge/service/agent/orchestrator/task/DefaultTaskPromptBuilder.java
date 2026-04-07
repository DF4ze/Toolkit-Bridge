package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.process.ExternalProcessService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.PromptProcessTemplate;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultTaskPromptBuilder implements TaskPromptBuilder {

    static final String TASK_EXECUTION_PROCESS_ID = "task-execution-prompt";

    private final ExternalProcessService externalProcessService;

    public DefaultTaskPromptBuilder(ExternalProcessService externalProcessService) {
        this.externalProcessService = externalProcessService;
    }

    @Override
    public TaskPrompt build(AgentRuntime runtime,
                            AgentRequest request,
                            OrchestrationRequestContext context,
                            Task task,
                            MemoryContext memoryContext) {
        PromptProcessTemplate template = externalProcessService.loadTypedContent(
                TASK_EXECUTION_PROCESS_ID,
                PromptProcessTemplate.class
        );

        Map<String, String> variables = Map.of(
                "baseSystemPrompt", runtime.definition().systemPrompt(),
                "taskObjective", task.objective(),
                "taskId", task.taskId(),
                "parentTaskId", task.parentTaskId() == null ? "none" : task.parentTaskId(),
                "traceId", task.traceId(),
                "entryPoint", task.entryPoint().name(),
                "memoryContextText", memoryContext.text()
        );

        return new TaskPrompt(
                externalProcessService.renderTemplate(template.systemPromptTemplate(), variables),
                externalProcessService.renderTemplate(template.userPromptTemplate(), variables)
        );
    }
}
