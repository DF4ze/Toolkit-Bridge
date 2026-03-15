package fr.ses10doigts.toolkitbridge.config.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "toolkit.llm.openai-like")
public class OpenAiLikeProvidersProperties {

    private List<OpenAiLikeProperties> providers = new ArrayList<>();

}