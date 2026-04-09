package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProvidersProperties;
import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.integration.config.MemoryIntegrationProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.scoring.config.MemoryScoringProperties;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.ArtifactsConfigurationPayload;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.RetentionConfigurationPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdministrableConfigurationSeedServiceTest {

    @Test
    void bootstrapShouldMergeProvidersByNameAndCompleteOnlyBlankFields() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();
        providersProperties.setProviders(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "seed-secret", "gpt-4.1-mini")
        ));

        List<OpenAiLikeProperties> existingProviders = List.of(
                new OpenAiLikeProperties("openai", "https://custom.local/v1", "", "")
        );
        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.of(existingProviders));
        when(storeService.read(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new ArtifactsConfigurationPayload()));
        when(storeService.read(eq(AdministrableConfigKey.RETENTION_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new RetentionConfigurationPayload()));
        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new MemoryConfigurationPayload()));

        AdministrableConfigurationSeedService seedService = buildSeedService(storeService, providersProperties);

        seedService.bootstrapSeedsIfMissing();

        verify(storeService, times(1)).write(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), argThat(payload -> {
            @SuppressWarnings("unchecked")
            List<OpenAiLikeProperties> providers = (List<OpenAiLikeProperties>) payload;
            if (providers.size() != 1) {
                return false;
            }
            OpenAiLikeProperties provider = providers.getFirst();
            return "openai".equals(provider.name())
                    && "https://custom.local/v1".equals(provider.baseUrl())
                    && "seed-secret".equals(provider.apiKey())
                    && "gpt-4.1-mini".equals(provider.defaultModel());
        }));
    }

    @Test
    void bootstrapShouldNotTouchAgentDefinitionsKey() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();
        providersProperties.setProviders(List.of());

        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(storeService.read(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new ArtifactsConfigurationPayload()));
        when(storeService.read(eq(AdministrableConfigKey.RETENTION_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new RetentionConfigurationPayload()));
        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new MemoryConfigurationPayload()));

        AdministrableConfigurationSeedService seedService = buildSeedService(storeService, providersProperties);

        seedService.bootstrapSeedsIfMissing();

        verify(storeService, never()).read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class));
        verify(storeService, never()).write(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any());
    }

    @Test
    void bootstrapShouldCreateMissingTechnicalAndMemoryKeys() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();
        providersProperties.setProviders(List.of());

        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(storeService.read(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.empty());
        when(storeService.read(eq(AdministrableConfigKey.RETENTION_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.empty());
        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        AdministrableConfigurationSeedService seedService = buildSeedService(storeService, providersProperties);

        boolean changed = seedService.bootstrapSeedsIfMissing();

        assertThat(changed).isTrue();
        verify(storeService, times(1)).write(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), any(ArtifactsConfigurationPayload.class));
        verify(storeService, times(1)).write(eq(AdministrableConfigKey.RETENTION_CONFIGURATION), any(RetentionConfigurationPayload.class));
        verify(storeService, times(1)).write(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(MemoryConfigurationPayload.class));
    }

    @Test
    void bootstrapShouldNormalizeExistingArtifactsRetentionKeys() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();
        providersProperties.setProviders(List.of());

        ArtifactsConfigurationPayload existingArtifacts = new ArtifactsConfigurationPayload();
        existingArtifacts.setEnabled(true);
        existingArtifacts.setContentFolder("artifacts");
        ArtifactsConfigurationPayload.Retention retention = new ArtifactsConfigurationPayload.Retention();
        retention.setDefaultTtl(Duration.ofHours(24));
        retention.setByType(new java.util.LinkedHashMap<>());
        retention.getByType().put("Report", Duration.ofHours(1));
        existingArtifacts.setRetention(retention);

        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(storeService.read(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(existingArtifacts));
        when(storeService.read(eq(AdministrableConfigKey.RETENTION_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new RetentionConfigurationPayload()));
        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(new MemoryConfigurationPayload()));

        AdministrableConfigurationSeedService seedService = buildSeedService(storeService, providersProperties);

        seedService.bootstrapSeedsIfMissing();

        verify(storeService).write(eq(AdministrableConfigKey.ARTIFACTS_CONFIGURATION), argThat(payload -> {
            ArtifactsConfigurationPayload p = (ArtifactsConfigurationPayload) payload;
            if (p.getRetention() == null || p.getRetention().getByType() == null) {
                return false;
            }
            return p.getRetention().getByType().containsKey("report")
                    && !p.getRetention().getByType().containsKey("Report");
        }));
    }

    private static AdministrableConfigurationSeedService buildSeedService(
            AdministrableConfigurationStoreService storeService,
            OpenAiLikeProvidersProperties providersProperties
    ) {
        return new AdministrableConfigurationSeedService(
                storeService,
                providersProperties,
                new ArtifactStorageProperties(),
                new PersistenceRetentionProperties(),
                new ConversationMemoryProperties(),
                new ContextAssemblerProperties(),
                new MemoryRetrievalProperties(),
                new MemoryIntegrationProperties(),
                new MemoryScoringProperties(),
                new GlobalContextProperties()
        );
    }
}
