package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolFunction;
import fr.ses10doigts.toolkitbridge.service.tool.ToolExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class AgentBashController {

    private final ToolExecutionService toolExecutionService;

    @PostMapping("/run")
    public ResponseEntity<ToolExecutionResult> runCommand(@Valid @RequestBody BashRequest request) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("command", request.getTool());
        arguments.put("timeout", String.valueOf(request.getTimeout()));
        if (request.getArgs() != null && !request.getArgs().isEmpty()) {
            arguments.put("args", request.getArgs());
        }

        ToolExecutionResult response = toolExecutionService.execute(
                new ToolCall("web-run-command", new ToolFunction("run_command", Map.copyOf(arguments)))
        );
        return ResponseEntity.status(response.isError() ? HttpStatus.BAD_REQUEST : HttpStatus.OK)
                .body(response);
    }
}
