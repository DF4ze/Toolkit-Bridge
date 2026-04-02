package fr.ses10doigts.toolkitbridge.memory.facade;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;

import java.util.List;

public interface MemoryFacade {

    MemoryContext buildContext(MemoryContextRequest request);

    void onUserMessage(MemoryContextRequest request);

    void onAssistantMessage(MemoryContextRequest request, String assistantMessage);

    void onToolExecution(MemoryContextRequest request, ToolExecutionRecord toolExecution);

    void markContextMemoriesUsed(List<Long> semanticMemoryIds);
}
