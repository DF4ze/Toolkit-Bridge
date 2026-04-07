package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolExecutionService {

    private final ToolRegistryService toolRegistryService;
    private final ToolArgumentValidator toolArgumentValidator;
    private final AgentPermissionControlService permissionControlService;

    public ToolExecutionResult execute(ToolCall toolCall) throws Exception{
        String toolName = toolCall.function().name();
        Map<String, Object> arguments = toolCall.function().arguments();

        permissionControlService.checkToolExecution(toolName);
        ToolHandler handler = toolRegistryService.getRequiredHandler(toolName);
        toolArgumentValidator.validate(handler.parametersSchema(), arguments);

        return handler.execute(arguments);
    }
}
