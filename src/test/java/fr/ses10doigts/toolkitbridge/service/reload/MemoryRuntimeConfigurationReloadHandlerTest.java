package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.context.global.port.SharedGlobalContextProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryRuntimeConfigurationReloadHandlerTest {

    @Test
    void shouldReturnSuccessAndInvalidateCacheWhenReloadSucceedsFromDatabase() {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        SharedGlobalContextProvider globalContextProvider = mock(SharedGlobalContextProvider.class);
        when(resolver.reloadFromDatabase()).thenReturn(true);

        MemoryRuntimeConfigurationReloadHandler handler = new MemoryRuntimeConfigurationReloadHandler(
                resolver,
                globalContextProvider
        );

        ReloadDomainResult result = handler.reload();

        assertThat(result.domain()).isEqualTo(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);
        assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(result.message()).contains("reloaded");
        verify(globalContextProvider).invalidateCache();
    }

    @Test
    void shouldReturnSuccessAndInvalidateCacheWhenReloadFallsBackToInternalDefaults() {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        SharedGlobalContextProvider globalContextProvider = mock(SharedGlobalContextProvider.class);
        when(resolver.reloadFromDatabase()).thenReturn(false);

        MemoryRuntimeConfigurationReloadHandler handler = new MemoryRuntimeConfigurationReloadHandler(
                resolver,
                globalContextProvider
        );

        ReloadDomainResult result = handler.reload();

        assertThat(result.domain()).isEqualTo(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);
        assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(result.message()).contains("internal defaults");
        verify(globalContextProvider).invalidateCache();
    }

    @Test
    void shouldNotInvalidateCacheWhenReloadFails() {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        SharedGlobalContextProvider globalContextProvider = mock(SharedGlobalContextProvider.class);
        when(resolver.reloadFromDatabase()).thenThrow(new IllegalStateException("boom"));

        MemoryRuntimeConfigurationReloadHandler handler = new MemoryRuntimeConfigurationReloadHandler(
                resolver,
                globalContextProvider
        );

        assertThatThrownBy(handler::reload)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
        verify(globalContextProvider, never()).invalidateCache();
    }

    @Test
    void shouldKeepReloadSuccessfulWhenCacheInvalidationThrows() {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        SharedGlobalContextProvider globalContextProvider = mock(SharedGlobalContextProvider.class);
        when(resolver.reloadFromDatabase()).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("cache boom"))
                .when(globalContextProvider)
                .invalidateCache();

        MemoryRuntimeConfigurationReloadHandler handler = new MemoryRuntimeConfigurationReloadHandler(
                resolver,
                globalContextProvider
        );

        ReloadDomainResult result = handler.reload();

        assertThat(result.domain()).isEqualTo(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);
        assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(result.message()).contains("cache invalidation failed");
        verify(globalContextProvider).invalidateCache();
    }
}
