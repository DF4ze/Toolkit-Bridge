package fr.ses10doigts.toolkitbridge.memory.semantic.service;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SemanticMemoryServiceTest {

    private MemoryEntryRepository repository;
    private SemanticMemoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(MemoryEntryRepository.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new DefaultSemanticMemoryService(repository, validator);
    }

    @Test
    void createValidMemoryEntry() {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId("agent-1");
        entry.setScope(MemoryScope.AGENT);
        entry.setType(MemoryType.FACT);
        entry.setContent("some content");
        entry.setImportance(0.8);
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setTags(Set.of("core"));

        when(repository.save(any(MemoryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemoryEntry saved = service.create(entry);

        assertThat(saved.getAgentId()).isEqualTo("agent-1");
        verify(repository, times(1)).save(any(MemoryEntry.class));
    }

    @Test
    void rejectInvalidMemoryEntry() {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId(" ");
        entry.setScope(MemoryScope.AGENT);
        entry.setType(MemoryType.FACT);
        entry.setContent("");

        assertThatThrownBy(() -> service.create(entry))
                .isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateMainFields() {
        MemoryEntry existing = new MemoryEntry();
        existing.setId(1L);
        existing.setAgentId("agent-1");
        existing.setScope(MemoryScope.AGENT);
        existing.setType(MemoryType.FACT);
        existing.setContent("old");
        existing.setImportance(0.1);
        existing.setStatus(MemoryStatus.ACTIVE);

        MemoryEntry updated = new MemoryEntry();
        updated.setAgentId("agent-2");
        updated.setScope(MemoryScope.PROJECT);
        updated.setScopeId("p1");
        updated.setType(MemoryType.DECISION);
        updated.setContent("new");
        updated.setImportance(0.9);
        updated.setStatus(MemoryStatus.ARCHIVED);
        updated.setTags(Set.of("tag-1"));

        when(repository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(MemoryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemoryEntry result = service.update(1L, updated);

        assertThat(result.getAgentId()).isEqualTo("agent-2");
        assertThat(result.getScope()).isEqualTo(MemoryScope.PROJECT);
        assertThat(result.getScopeId()).isEqualTo("p1");
        assertThat(result.getType()).isEqualTo(MemoryType.DECISION);
        assertThat(result.getContent()).isEqualTo("new");
        assertThat(result.getImportance()).isEqualTo(0.9);
        assertThat(result.getStatus()).isEqualTo(MemoryStatus.ARCHIVED);
        assertThat(result.getTags()).containsExactly("tag-1");
    }

    @Test
    void archiveMemoryEntry() {
        MemoryEntry existing = new MemoryEntry();
        existing.setId(2L);
        existing.setAgentId("agent-1");
        existing.setScope(MemoryScope.AGENT);
        existing.setType(MemoryType.FACT);
        existing.setContent("content");
        existing.setStatus(MemoryStatus.ACTIVE);

        when(repository.findById(2L)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(MemoryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.archive(2L);

        ArgumentCaptor<MemoryEntry> captor = ArgumentCaptor.forClass(MemoryEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MemoryStatus.ARCHIVED);
    }
}
