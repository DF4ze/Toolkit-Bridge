package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.llm.runtime.LlmProviderRegistryRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/llm-reload-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.llm.openai-like.providers[0].name=seed",
                "toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                "toolkit.llm.openai-like.providers[0].api-key=",
                "toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LlmProviderRegistryReloadSpringIT {

    @Autowired
    private ExplicitReloadService explicitReloadService;

    @Autowired
    private AdministrableConfigurationGateway configurationGateway;

    @Autowired
    private LlmProviderRegistryRuntime llmProviderRegistryRuntime;

    @Test
    void shouldReloadLlmProviderRegistryDomainThroughSpringHandlerAndUpdateRuntimeSnapshot() {
        assertThat(llmProviderRegistryRuntime.snapshot().exists("seed")).isTrue();

        configurationGateway.saveOpenAiLikeProviders(List.of(
                new OpenAiLikeProperties(
                        "reloaded",
                        "http://127.0.0.1:11434/v1",
                        "",
                        "qwen3.5:9b"
                )
        ));

        ReloadReport report = explicitReloadService.reload(ReloadDomain.LLM_PROVIDER_REGISTRY);
        ReloadDomainResult result = report.domainResults().stream()
                .filter(domainResult -> domainResult.domain() == ReloadDomain.LLM_PROVIDER_REGISTRY)
                .findFirst()
                .orElseThrow();

        assertThat(report.status()).isEqualTo(ReloadReportStatus.SUCCESS);
        assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(llmProviderRegistryRuntime.snapshot().exists("reloaded")).isTrue();
        assertThat(llmProviderRegistryRuntime.snapshot().exists("seed")).isFalse();
    }
}
