package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db"
})
class AdministrableConfigurationEntityVersionIT {

    @Autowired
    private AdministrableConfigurationRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldIncrementVersionOnUpdate() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String configKey = "test.version.increment." + UUID.randomUUID();

        Long id = tx.execute(status -> {
            AdministrableConfigurationEntity entity = new AdministrableConfigurationEntity();
            entity.setConfigKey(configKey);
            entity.setPayloadJson("{\"value\":1}");
            return repository.save(entity).getId();
        });

        Long initialVersion = tx.execute(status -> repository.findById(id).orElseThrow().getVersion());

        tx.executeWithoutResult(status -> {
            AdministrableConfigurationEntity current = repository.findById(id).orElseThrow();
            current.setPayloadJson("{\"value\":2}");
            repository.save(current);
        });

        Long updatedVersion = tx.execute(status -> repository.findById(id).orElseThrow().getVersion());

        assertThat(initialVersion).isNotNull();
        assertThat(updatedVersion).isGreaterThan(initialVersion);
    }

    @Test
    void shouldFailOnStaleUpdateWithOptimisticLocking() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String configKey = "test.version.conflict." + UUID.randomUUID();

        Long id = tx.execute(status -> {
            AdministrableConfigurationEntity entity = new AdministrableConfigurationEntity();
            entity.setConfigKey(configKey);
            entity.setPayloadJson("{\"value\":1}");
            return repository.save(entity).getId();
        });

        AdministrableConfigurationEntity stale = tx.execute(status -> repository.findById(id).orElseThrow());

        tx.executeWithoutResult(status -> {
            AdministrableConfigurationEntity current = repository.findById(id).orElseThrow();
            current.setPayloadJson("{\"value\":2}");
            repository.saveAndFlush(current);
        });

        stale.setPayloadJson("{\"value\":3}");

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.saveAndFlush(stale)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
