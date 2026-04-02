package fr.ses10doigts.toolkitbridge.memory.episodic.factory;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultEpisodicEventFactoryTest {

    private final DefaultEpisodicEventFactory factory = new DefaultEpisodicEventFactory();

    @Test
    void createsSuccessToolExecutionEvent() {
        EpisodeEvent event = factory.toolExecutionEvent(request(), new ToolExecutionRecord("read_file", true, "ok"));

        assertThat(event.getAction()).isEqualTo("tool_execution_success");
        assertThat(event.getType()).isEqualTo(EpisodeEventType.ACTION);
        assertThat(event.getStatus()).isEqualTo(EpisodeStatus.SUCCESS);
    }

    @Test
    void createsFailureToolExecutionEvent() {
        EpisodeEvent event = factory.toolExecutionEvent(request(), new ToolExecutionRecord("read_file", false, "boom"));

        assertThat(event.getAction()).isEqualTo("tool_execution_failure");
        assertThat(event.getType()).isEqualTo(EpisodeEventType.ERROR);
        assertThat(event.getStatus()).isEqualTo(EpisodeStatus.FAILURE);
    }

    private MemoryContextRequest request() {
        return new MemoryContextRequest("agent-1", "user-1", "bot-1", null, "hello", "conv-1", null, null, null, null);
    }
}
