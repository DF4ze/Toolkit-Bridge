package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryRuntimeConfigurationResolverTest {

    @Test
    void shouldFailWhenSnapshotRequestedBeforeInitialization() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(
                gateway,
                new MemoryRuntimeConfigurationMapper()
        );

        assertThatThrownBy(resolver::snapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }

    @Test
    void shouldInitializeFromDatabasePayload() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();
        payload.getContext().setMaxRules(3);
        payload.getRetrieval().setMaxCandidatePoolSize(12);
        payload.getIntegration().setEnableSemanticExtraction(Boolean.FALSE);
        payload.getScoring().setImportanceWeight(2.5);
        payload.getGlobalContext().setLoadMode("CACHED");
        payload.getGlobalContext().setCacheRefreshInterval(Duration.ofMinutes(2));
        when(gateway.loadMemoryConfiguration()).thenReturn(Optional.of(payload));

        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(
                gateway,
                new MemoryRuntimeConfigurationMapper()
        );
        resolver.run(mock(ApplicationArguments.class));

        MemoryRuntimeConfiguration snapshot = resolver.snapshot();
        assertThat(snapshot.context().maxRules()).isEqualTo(3);
        assertThat(snapshot.retrieval().maxCandidatePoolSize()).isEqualTo(12);
        assertThat(snapshot.integration().enableSemanticExtraction()).isFalse();
        assertThat(snapshot.scoring().importanceWeight()).isEqualTo(2.5);
        assertThat(snapshot.globalContext().loadMode()).isEqualTo(MemoryRuntimeConfiguration.GlobalContextLoadMode.CACHED);
        assertThat(snapshot.globalContext().cacheRefreshInterval()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUseInternalDefaultsWhenDatabasePayloadMissing() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        when(gateway.loadMemoryConfiguration()).thenReturn(Optional.empty());

        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(
                gateway,
                new MemoryRuntimeConfigurationMapper()
        );
        resolver.run(mock(ApplicationArguments.class));

        MemoryRuntimeConfiguration snapshot = resolver.snapshot();
        assertThat(snapshot).isEqualTo(MemoryRuntimeConfiguration.defaults());
    }

    @Test
    void shouldCompletePartialOrInvalidPayloadWithInternalDefaults() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();
        payload.setContext(null);
        payload.getRetrieval().setMaxCandidatePoolSize(0);
        payload.getScoring().setRecencyWeight(Double.NaN);
        payload.getGlobalContext().setCacheRefreshInterval(Duration.ZERO);
        payload.getGlobalContext().setLoadMode("unsupported");
        when(gateway.loadMemoryConfiguration()).thenReturn(Optional.of(payload));

        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(
                gateway,
                new MemoryRuntimeConfigurationMapper()
        );
        resolver.run(mock(ApplicationArguments.class));

        MemoryRuntimeConfiguration snapshot = resolver.snapshot();
        assertThat(snapshot.context()).isEqualTo(MemoryRuntimeConfiguration.defaults().context());
        assertThat(snapshot.retrieval().maxCandidatePoolSize())
                .isEqualTo(MemoryRuntimeConfiguration.defaults().retrieval().maxCandidatePoolSize());
        assertThat(snapshot.scoring().recencyWeight())
                .isEqualTo(MemoryRuntimeConfiguration.defaults().scoring().recencyWeight());
        assertThat(snapshot.globalContext().cacheRefreshInterval())
                .isEqualTo(MemoryRuntimeConfiguration.defaults().globalContext().cacheRefreshInterval());
        assertThat(snapshot.globalContext().loadMode())
                .isEqualTo(MemoryRuntimeConfiguration.defaults().globalContext().loadMode());
    }

    @Test
    void shouldReloadAndSwapSnapshotAfterSuccessfulBuildAndValidation() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);

        MemoryConfigurationPayload initialPayload = new MemoryConfigurationPayload();
        initialPayload.getContext().setMaxRules(3);

        MemoryConfigurationPayload reloadedPayload = new MemoryConfigurationPayload();
        reloadedPayload.getContext().setMaxRules(9);

        when(gateway.loadMemoryConfiguration()).thenReturn(Optional.of(initialPayload), Optional.of(reloadedPayload));

        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(
                gateway,
                new MemoryRuntimeConfigurationMapper()
        );
        resolver.run(mock(ApplicationArguments.class));

        assertThat(resolver.snapshot().context().maxRules()).isEqualTo(3);

        boolean loadedFromDatabase = resolver.reloadFromDatabase();

        assertThat(loadedFromDatabase).isTrue();
        assertThat(resolver.snapshot().context().maxRules()).isEqualTo(9);
    }

    @Test
    void shouldKeepPreviousSnapshotWhenReloadValidationFails() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        when(gateway.loadMemoryConfiguration()).thenReturn(Optional.of(new MemoryConfigurationPayload()), Optional.of(new MemoryConfigurationPayload()));

        MemoryRuntimeConfigurationMapper mapper = mock(MemoryRuntimeConfigurationMapper.class);
        MemoryRuntimeConfiguration validConfiguration = MemoryRuntimeConfiguration.defaults();
        MemoryRuntimeConfiguration invalidConfiguration = new MemoryRuntimeConfiguration(
                new MemoryRuntimeConfiguration.Context(0, 10, 15000, 5),
                validConfiguration.retrieval(),
                validConfiguration.integration(),
                validConfiguration.scoring(),
                validConfiguration.globalContext()
        );
        when(mapper.toRuntimeConfiguration(any())).thenReturn(validConfiguration, invalidConfiguration);

        MemoryRuntimeConfigurationResolver resolver = new MemoryRuntimeConfigurationResolver(gateway, mapper);
        resolver.run(mock(ApplicationArguments.class));

        MemoryRuntimeConfiguration beforeReloadFailure = resolver.snapshot();

        assertThatThrownBy(resolver::reloadFromDatabase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid memory runtime context configuration");
        assertThat(resolver.snapshot()).isEqualTo(beforeReloadFailure);
    }
}
