package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
public class AdministrableConfigurationStoreService {

    private final AdministrableConfigurationRepository repository;
    private final ObjectMapper objectMapper;

    public AdministrableConfigurationStoreService(
            AdministrableConfigurationRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> read(AdministrableConfigKey key, TypeReference<T> valueType) {
        return repository.findByConfigKey(key.storageKey())
                .map(AdministrableConfigurationEntity::getPayloadJson)
                .map(payload -> deserialize(key, payload, valueType));
    }

    @Transactional
    public <T> void write(AdministrableConfigKey key, T payload) {
        AdministrableConfigurationEntity entity = repository.findByConfigKey(key.storageKey())
                .orElseGet(AdministrableConfigurationEntity::new);
        entity.setConfigKey(key.storageKey());
        entity.setPayloadJson(serialize(key, payload));
        repository.save(entity);
    }

    private <T> String serialize(AdministrableConfigKey key, T payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize administrable configuration key=" + key.storageKey(), e);
        }
    }

    private <T> T deserialize(AdministrableConfigKey key, String payloadJson, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(payloadJson, valueType);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize administrable configuration key=" + key.storageKey(), e);
        }
    }
}

