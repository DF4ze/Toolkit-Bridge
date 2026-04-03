package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import org.springframework.stereotype.Component;

@Component
public class DefaultTaskPromptBuilder implements TaskPromptBuilder {

    @Override
    public TaskPrompt build(AgentRuntime runtime,
                            AgentRequest request,
                            OrchestrationRequestContext context,
                            Task task,
                            MemoryContext memoryContext) {

        String systemPrompt = runtime.definition().systemPrompt() + "\n\n"
                + "You are operating in TASK mode. Focus on objective execution.\n"
                + "Keep your response actionable, explicit, and concise.\n"
                + "Use tools only when they are necessary and available.\n"
                + "Do not invent delegated subtasks: describe next execution steps directly.";

        // Extension point for future task model enrichment (subtasks, delegates, planners).
        String userPrompt = "Task objective:\n"
                + task.objective()
                + "\n\nTask metadata:\n"
                + "taskId=" + task.taskId()
                + ", parentTaskId=" + (task.parentTaskId() == null ? "none" : task.parentTaskId())
                + ", traceId=" + task.traceId()
                + ", entryPoint=" + task.entryPoint()
                + "\n\nExecution contract:\n"
                + "1. Restate the objective briefly.\n"
                + "2. Provide a concrete step-by-step execution flow.\n"
                + "3. Return a structured final outcome.\n\n"
                + "Conversation and memory context:\n"
                + memoryContext.text();

        return new TaskPrompt(systemPrompt, userPrompt);
    }
}
