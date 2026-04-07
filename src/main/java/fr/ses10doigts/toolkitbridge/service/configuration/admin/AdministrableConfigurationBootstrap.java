package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AdministrableConfigurationBootstrap implements ApplicationRunner {

    private final AdministrableConfigurationGateway gateway;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        boolean seeded = gateway.bootstrapSeedsIfMissing();
        if (seeded) {
            log.info("Administrable configuration bootstrap completed from YAML seed");
            return;
        }
        log.info("Administrable configuration bootstrap skipped: DB configuration already initialized");
    }
}

