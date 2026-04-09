package fr.ses10doigts.toolkitbridge.config.agent;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "toolkit.agents")
public class AgentsProperties {

    @Valid
    private List<AgentDefinitionProperties> definitions = new ArrayList<>();
}
