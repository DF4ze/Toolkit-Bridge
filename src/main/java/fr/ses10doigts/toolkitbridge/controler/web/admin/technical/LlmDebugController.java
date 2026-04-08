package fr.ses10doigts.toolkitbridge.controler.web.admin.technical;

import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugSnapshot;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/technical/llms")
@RequiredArgsConstructor
public class LlmDebugController {

    private final LlmDebugStore llmDebugStore;

    @GetMapping("/debug/{agentId}")
    public ResponseEntity<LlmDebugSnapshot> getDebug(@PathVariable String agentId) {
        return llmDebugStore.get(agentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
