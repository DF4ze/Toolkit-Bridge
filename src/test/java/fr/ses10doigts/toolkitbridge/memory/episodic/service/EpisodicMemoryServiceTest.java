package fr.ses10doigts.toolkitbridge.memory.episodic.service;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.repository.EpisodeEventRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EpisodicMemoryServiceTest {

    private static Validator validator;
    private EpisodeEventRepository repository;
    private EpisodicMemoryService service;

    @BeforeAll
    static void beforeAll() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @BeforeEach
    void setUp() {
        repository = mock(EpisodeEventRepository.class);
        service = new DefaultEpisodicMemoryService(repository, validator);
    }

    @Test
    void recordValidEvent() {
        EpisodeEvent event = validEvent();

        when(repository.save(any(EpisodeEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EpisodeEvent saved = service.record(event);

        assertThat(saved.getAgentId()).isEqualTo("agent-1");
        verify(repository).save(any(EpisodeEvent.class));
    }

    @Test
    void rejectInvalidEvent() {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(" ");
        event.setScope(EpisodeScope.AGENT);
        event.setType(EpisodeEventType.ACTION);
        event.setAction("");
        event.setStatus(EpisodeStatus.UNKNOWN);

        assertThatThrownBy(() -> service.record(event))
                .isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findRecentByAgentDelegatesToRepository() {
        when(repository.findByAgentIdOrderByCreatedAtDesc(eq("agent-1"), any()))
                .thenReturn(List.of(validEvent()));

        List<EpisodeEvent> result = service.findRecent("agent-1", 5);

        assertThat(result).hasSize(1);
        verify(repository).findByAgentIdOrderByCreatedAtDesc(eq("agent-1"), any());
    }

    @Test
    void findRecentByScopeDelegatesToRepository() {
        when(repository.findByAgentIdAndScopeOrderByCreatedAtDesc(eq("agent-1"), eq(EpisodeScope.PROJECT), any()))
                .thenReturn(List.of(validEvent()));

        List<EpisodeEvent> result = service.findRecentByScope("agent-1", EpisodeScope.PROJECT, 3);

        assertThat(result).hasSize(1);
        ArgumentCaptor<EpisodeScope> captor = ArgumentCaptor.forClass(EpisodeScope.class);
        verify(repository).findByAgentIdAndScopeOrderByCreatedAtDesc(eq("agent-1"), captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(EpisodeScope.PROJECT);
    }

    @Test
    void rejectsInvalidLimit() {
        assertThatThrownBy(() -> service.findRecent("agent-1", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private EpisodeEvent validEvent() {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId("agent-1");
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId("project-1");
        event.setType(EpisodeEventType.ACTION);
        event.setAction("EXECUTE_TOOL");
        event.setDetails("Ran tool X");
        event.setStatus(EpisodeStatus.SUCCESS);
        event.setScore(1.5);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
