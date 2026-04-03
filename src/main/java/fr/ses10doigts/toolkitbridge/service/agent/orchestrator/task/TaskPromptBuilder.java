package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;

public interface TaskPromptBuilder {
    TaskPrompt build(AgentRuntime runtime,
                     AgentRequest request,
                     OrchestrationRequestContext context,
                     Task task,
                     MemoryContext memoryContext);
}
