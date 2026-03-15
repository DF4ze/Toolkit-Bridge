package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.web.CommandRequest;
import fr.ses10doigts.toolkitbridge.service.tool.bash.BotBashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class BotBashController {

    private final BotBashService botBashService;

    @PostMapping("/run")
    public ResponseEntity<ToolExecutionResult> runCommand(@Valid @RequestBody CommandRequest request) throws Exception {
             ToolExecutionResult response = botBashService.execute(request);
         return ResponseEntity.status(response.isError() ? HttpStatus.BAD_REQUEST : HttpStatus.OK)
                    .body(response);
    }
}