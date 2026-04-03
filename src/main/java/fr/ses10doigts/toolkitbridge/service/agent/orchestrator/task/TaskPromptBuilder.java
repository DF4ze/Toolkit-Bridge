package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

public interface TaskPromptBuilder {
    TaskPrompt build(AgentRuntime runtime,
                     AgentRequest request,
                     OrchestrationRequestContext context,
                     MemoryContext memoryContext);
}
