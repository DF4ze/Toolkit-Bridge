package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.*;

class AdministrableConfigurationBootstrapTest {

    @Test
    void shouldInvokeSeedBootstrap() {
        AdministrableConfigurationSeedService seedService = mock(AdministrableConfigurationSeedService.class);
        when(seedService.bootstrapSeedsIfMissing()).thenReturn(true);

        AdministrableConfigurationBootstrap bootstrap = new AdministrableConfigurationBootstrap(seedService);
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(seedService, times(1)).bootstrapSeedsIfMissing();
    }
}

