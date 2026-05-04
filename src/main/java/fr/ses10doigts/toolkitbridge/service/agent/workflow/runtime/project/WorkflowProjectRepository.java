package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowProjectRepository extends JpaRepository<WorkflowProjectEntity, Long> {

    Optional<WorkflowProjectEntity> findByProjectKey(String projectKey);

    boolean existsByProjectKey(String projectKey);
}

