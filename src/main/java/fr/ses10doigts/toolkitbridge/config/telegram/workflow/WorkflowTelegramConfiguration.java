package fr.ses10doigts.toolkitbridge.config.telegram.workflow;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableConfigurationProperties(WorkflowTelegramProperties.class)
public class WorkflowTelegramConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService workflowExecutor() {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory("workflow-telegram"));
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix == null || prefix.isBlank() ? "workflow-telegram" : prefix.trim();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(prefix + "-" + t.getId());
            t.setDaemon(true);
            return t;
        }
    }
}

