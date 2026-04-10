package fr.ses10doigts.toolkitbridge.persistence.retention;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PersistenceRetentionCleanupSchedulerTest {

    @Test
    void cleanupPersistenceDataDelegatesToCleanupService() {
        PersistenceRetentionCleanupService cleanupService = mock(PersistenceRetentionCleanupService.class);

        PersistenceRetentionCleanupScheduler scheduler = new PersistenceRetentionCleanupScheduler(cleanupService);

        scheduler.cleanupPersistenceData();

        verify(cleanupService).cleanup();
    }

    @Test
    void schedulerBeanIsCreatedOnlyWhenEnabledPropertyAllowsIt() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withUserConfiguration(TestConfiguration.class, PersistenceRetentionCleanupScheduler.class);

        contextRunner
                .withPropertyValues(
                        "toolkit.persistence.retention.cleanup.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(PersistenceRetentionCleanupScheduler.class));

        contextRunner
                .withPropertyValues(
                        "toolkit.persistence.retention.cleanup.enabled=false",
                        "toolkit.persistence.retention.cleanup.cron=0 0 3 * * *"
                )
                .run(context -> assertThat(context).doesNotHaveBean(PersistenceRetentionCleanupScheduler.class));
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        PersistenceRetentionCleanupService persistenceRetentionCleanupService() {
            return mock(PersistenceRetentionCleanupService.class);
        }
    }
}
