package fr.ses10doigts.toolkitbridge.controler.web.admin.technical;

import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugSnapshot;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmDebugControllerTest {

    @Test
    void returnsSnapshotWhenFoundOtherwiseNotFound() {
        LlmDebugStore store = mock(LlmDebugStore.class);
        LlmDebugController controller = new LlmDebugController(store);

        LlmDebugSnapshot snapshot = new LlmDebugSnapshot(
                "agent-1",
                "openai",
                "gpt-5",
                true,
                "trace-1",
                Instant.parse("2026-01-01T00:00:00Z"),
                "system",
                "user",
                "response",
                null
        );
        when(store.get("agent-1")).thenReturn(Optional.of(snapshot));
        when(store.get("agent-2")).thenReturn(Optional.empty());

        ResponseEntity<LlmDebugSnapshot> found = controller.getDebug("agent-1");
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).isEqualTo(snapshot);

        ResponseEntity<LlmDebugSnapshot> missing = controller.getDebug("agent-2");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNull();
    }
}
