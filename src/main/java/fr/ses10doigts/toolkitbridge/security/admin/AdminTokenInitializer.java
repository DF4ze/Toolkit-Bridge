package fr.ses10doigts.toolkitbridge.security.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminTokenInitializer implements ApplicationRunner {

    private final AdminTokenService adminTokenService;

    @Override
    public void run(ApplicationArguments args) {
        adminTokenService.initializeTokenIfNeeded();
    }
}
