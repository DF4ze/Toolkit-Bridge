package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LlmAdminControllerTest {

    @Test
    void delegatesListAndGetToFacade() {
        LlmAdminFacade facade = mock(LlmAdminFacade.class);
        LlmAdminController controller = new LlmAdminController(facade);

        LlmAdminResponse llm = new LlmAdminResponse(
                "openai",
                "https://api.openai.com/v1",
                "gpt-5",
                true
        );

        when(facade.listLlms()).thenReturn(List.of(llm));
        when(facade.getLlm("openai")).thenReturn(Optional.of(llm));
        when(facade.getLlm("missing")).thenReturn(Optional.empty());

        assertThat(controller.listLlms()).containsExactly(llm);

        ResponseEntity<LlmAdminResponse> found = controller.getLlm("openai");
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).isEqualTo(llm);

        ResponseEntity<LlmAdminResponse> missing = controller.getLlm("missing");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNull();

        verify(facade).listLlms();
        verify(facade).getLlm("openai");
        verify(facade).getLlm("missing");
    }
}
