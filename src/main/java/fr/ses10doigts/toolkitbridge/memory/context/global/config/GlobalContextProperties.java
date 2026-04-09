package fr.ses10doigts.toolkitbridge.memory.context.global.config;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryConfigurationDefaults;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "toolkit.memory.context.global")
public class GlobalContextProperties {

    private boolean enabled = MemoryConfigurationDefaults.GLOBAL_CONTEXT_ENABLED;

    @NotNull
    private LoadMode loadMode = LoadMode.valueOf(MemoryConfigurationDefaults.GLOBAL_CONTEXT_LOAD_MODE.name());

    @NotNull
    private Duration cacheRefreshInterval = MemoryConfigurationDefaults.GLOBAL_CONTEXT_CACHE_REFRESH_INTERVAL;

    private List<String> files = new ArrayList<>(MemoryConfigurationDefaults.GLOBAL_CONTEXT_FILES);

    public enum LoadMode {
        ON_DEMAND,
        CACHED
    }
}
