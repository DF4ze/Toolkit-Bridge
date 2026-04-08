package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/llms")
@RequiredArgsConstructor
public class LlmAdminController {

    private final LlmAdminFacade llmAdminFacade;

    @GetMapping
    public List<LlmAdminResponse> listLlms() {
        return llmAdminFacade.listLlms();
    }

    @GetMapping("/{llmId}")
    public ResponseEntity<LlmAdminResponse> getLlm(@PathVariable String llmId) {
        return ResponseEntity.of(llmAdminFacade.getLlm(llmId));
    }
}
