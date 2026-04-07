package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.*;

class AdministrableConfigurationBootstrapTest {

    @Test
    void shouldInvokeGatewayBootstrap() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        when(gateway.bootstrapSeedsIfMissing()).thenReturn(true);

        AdministrableConfigurationBootstrap bootstrap = new AdministrableConfigurationBootstrap(gateway);
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(gateway, times(1)).bootstrapSeedsIfMissing();
    }
}

