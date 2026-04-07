package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministrableConfigurationStoreServiceTest {

    @Test
    void shouldDeserializePayloadWhenRecordExists() {
        AdministrableConfigurationRepository repository = mock(AdministrableConfigurationRepository.class);
        AdministrableConfigurationEntity entity = new AdministrableConfigurationEntity();
        entity.setConfigKey(AdministrableConfigKey.AGENT_DEFINITIONS.storageKey());
        entity.setPayloadJson("[{\"id\":\"cortex\"}]");
        when(repository.findByConfigKey(AdministrableConfigKey.AGENT_DEFINITIONS.storageKey()))
                .thenReturn(Optional.of(entity));

        AdministrableConfigurationStoreService service = new AdministrableConfigurationStoreService(
                repository,
                new ObjectMapper()
        );

        Optional<List<RecordView>> value = service.read(
                AdministrableConfigKey.AGENT_DEFINITIONS,
                new TypeReference<>() {
                }
        );

        assertThat(value).isPresent();
        assertThat(value.get()).extracting(RecordView::id).containsExactly("cortex");
    }

    @Test
    void shouldUpsertPayloadWhenWriting() {
        AdministrableConfigurationRepository repository = mock(AdministrableConfigurationRepository.class);
        when(repository.findByConfigKey(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS.storageKey()))
                .thenReturn(Optional.empty());
        when(repository.save(any(AdministrableConfigurationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdministrableConfigurationStoreService service = new AdministrableConfigurationStoreService(
                repository,
                new ObjectMapper()
        );

        service.write(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS, List.of(new RecordView("openai")));

        verify(repository).save(any(AdministrableConfigurationEntity.class));
    }

    private record RecordView(String id) {
    }
}

