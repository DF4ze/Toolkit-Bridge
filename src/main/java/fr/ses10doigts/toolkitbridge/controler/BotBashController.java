package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.web.CommandRequest;
import fr.ses10doigts.toolkitbridge.model.dto.web.CommandResponse;
import fr.ses10doigts.toolkitbridge.service.botservice.BotBashService;
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
    public ResponseEntity<CommandResponse> runCommand(@Valid @RequestBody CommandRequest request) {
        try {
            CommandResponse response = botBashService.execute(request);
            return ResponseEntity.status(response.isError() ? HttpStatus.BAD_REQUEST : HttpStatus.OK)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new CommandResponse(true, e.getMessage(), "", "", null, false));
        } catch (ForbiddenCommandException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommandResponse(true, e.getMessage(), "", "", null, false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommandResponse(true, "Internal server error", "", "", null, false));
        }
    }
}