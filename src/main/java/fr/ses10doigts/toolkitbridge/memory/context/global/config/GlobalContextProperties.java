package fr.ses10doigts.toolkitbridge.memory.context.global.config;

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

    private boolean enabled = true;

    @NotNull
    private LoadMode loadMode = LoadMode.ON_DEMAND;

    @NotNull
    private Duration cacheRefreshInterval = Duration.ofSeconds(30);

    private List<String> files = new ArrayList<>();

    public enum LoadMode {
        ON_DEMAND,
        CACHED
    }
}
