package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db"
})
@Transactional
class WorkflowProjectRepositoryIT {

    @Autowired
    private WorkflowProjectRepository repository;

    @Test
    void saveAndFindByProjectKey() throws Exception {
        Path dir = Files.createTempDirectory("workflow-project-it");

        WorkflowProjectEntity entity = new WorkflowProjectEntity();
        entity.setProjectKey("toolkitbridge");
        entity.setProjectName("ToolkitBridge");
        entity.setProjectPath(dir.toAbsolutePath().normalize().toString());

        repository.save(entity);

        Optional<WorkflowProjectEntity> found = repository.findByProjectKey("toolkitbridge");
        assertThat(found).isPresent();
        assertThat(found.get().getProjectName()).isEqualTo("ToolkitBridge");
    }
}

