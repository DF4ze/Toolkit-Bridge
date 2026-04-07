package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministrableConfigurationRepository extends JpaRepository<AdministrableConfigurationEntity, Long> {

    Optional<AdministrableConfigurationEntity> findByConfigKey(String configKey);
}

