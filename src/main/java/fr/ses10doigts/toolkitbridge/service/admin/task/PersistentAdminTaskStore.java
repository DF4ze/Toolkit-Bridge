package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Component
@Primary
@Slf4j
public class PersistentAdminTaskStore implements AdminTaskStore {

    private final AdminTaskSnapshotRepository repository;
    private final AdminTaskSnapshotMapper mapper;

    public PersistentAdminTaskStore(AdminTaskSnapshotRepository repository,
                                    AdminTaskSnapshotMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void record(Task task, String channelType, String conversationId, String errorMessage) {
        if (task == null) {
            return;
        }

        Instant now = Instant.now();
        String normalizedAgentId = mapper.normalizeAgentId(task.assignedAgentId());
        String normalizedErrorMessage = mapper.normalizeMessage(errorMessage);
        int artifactCount = task.artifacts() == null ? 0 : task.artifacts().size();

        int updated = repository.updateSnapshotByTaskId(
                task.taskId(),
                task.parentTaskId(),
                task.objective(),
                task.initiator(),
                normalizedAgentId,
                task.traceId(),
                task.entryPoint(),
                task.status(),
                channelType,
                conversationId,
                now,
                normalizedErrorMessage,
                artifactCount
        );

        if (updated > 0) {
            return;
        }

        AdminTaskSnapshotEntity entity = mapper.toEntityForInsert(task, channelType, conversationId, normalizedErrorMessage, now);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            int updatedAfterConflict = repository.updateSnapshotByTaskId(
                    task.taskId(),
                    task.parentTaskId(),
                    task.objective(),
                    task.initiator(),
                    normalizedAgentId,
                    task.traceId(),
                    task.entryPoint(),
                    task.status(),
                    channelType,
                    conversationId,
                    now,
                    normalizedErrorMessage,
                    artifactCount
            );
            if (updatedAfterConflict == 0) {
                log.warn("Admin task snapshot write lost after insert conflict taskId={}", task.taskId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTaskSnapshot> recent(int limit, String agentId, TaskStatus status) {
        int effectiveLimit = Math.max(limit, 1);
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);
        String normalizedAgentId = mapper.normalizeAgentId(agentId);

        List<AdminTaskSnapshotEntity> entities;
        if (normalizedAgentId != null && status != null) {
            entities = repository.findByAssignedAgentIdAndStatusOrderByLastSeenAtDescTaskIdDesc(normalizedAgentId, status, pageRequest);
        } else if (normalizedAgentId != null) {
            entities = repository.findByAssignedAgentIdOrderByLastSeenAtDescTaskIdDesc(normalizedAgentId, pageRequest);
        } else if (status != null) {
            entities = repository.findByStatusOrderByLastSeenAtDescTaskIdDesc(status, pageRequest);
        } else {
            entities = repository.findByOrderByLastSeenAtDescTaskIdDesc(pageRequest);
        }

        return entities.stream().map(mapper::toDomain).toList();
    }
}
