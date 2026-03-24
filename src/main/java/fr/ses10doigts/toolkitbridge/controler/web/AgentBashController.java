package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashRequest;
import fr.ses10doigts.toolkitbridge.service.tool.bash.BashToolHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class AgentBashController {

    private final BashToolHandler bashToolHandler;

    @PostMapping("/run")
    public ResponseEntity<ToolExecutionResult> runCommand(@Valid @RequestBody BashRequest request) throws Exception {
             ToolExecutionResult response = bashToolHandler.execute(request);
         return ResponseEntity.status(response.isError() ? HttpStatus.BAD_REQUEST : HttpStatus.OK)
                    .body(response);
    }
}