package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentPolicyProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentAdminPageView;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentDefinitionAdminForm;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentDefinitionAdminFacade {

    private final AgentDefinitionService agentDefinitionService;
    private final AgentAccountAdminService agentAccountAdminService;
    private final AdministrableConfigurationGateway configurationGateway;
    private final LlmAdminFacade llmAdminFacade;
    private final TelegramBotAdminFacade telegramBotAdminFacade;

    public List<AgentAdminPageView.AgentItem> listAgents() {
        return toViewItems(agentDefinitionService.findAll());
    }

    public Optional<AgentAdminPageView.AgentItem> getAgent(String agentId) {
        String normalizedAgentId = normalize(agentId);
        if (normalizedAgentId == null) {
            return Optional.empty();
        }
        return agentDefinitionService.findById(normalizedAgentId)
                .map(definition -> toViewItem(definition, accountStateByAgentId()));
    }

    public List<LlmAdminResponse> listLlmOptions() {
        return llmAdminFacade.listLlms();
    }

    public List<TelegramBotAdminResponse> listTelegramBotOptions() {
        return telegramBotAdminFacade.listTelegramBots();
    }

    public AgentAdminPageView.AgentItem createAgentDefinition(AgentDefinitionAdminForm form) {
        String normalizedAgentId = requireNonBlank(form.getAgentId(), "agentId");
        ensureAssociationsExist(form.getLlmProvider(), form.getTelegramBotId());

        List<AgentDefinitionProperties> definitions = new ArrayList<>(configurationGateway.loadAgentDefinitions());
        boolean alreadyExists = definitions.stream()
                .anyMatch(definition -> normalizedAgentId.equals(normalize(definition.getId())));
        if (alreadyExists) {
            throw new AgentAdminValidationException("An agent with this id already exists.");
        }

        AgentDefinitionProperties created = toProperties(form, normalizedAgentId, null);
        definitions.add(created);
        configurationGateway.saveAgentDefinitions(definitions);
        return toViewItem(AgentDefinition.fromProperties(created), accountStateByAgentId());
    }

    public Optional<AgentAdminPageView.AgentItem> updateAgentDefinition(String agentId, AgentDefinitionAdminForm form) {
        String normalizedAgentId = requireNonBlank(agentId, "agentId");
        ensureAssociationsExist(form.getLlmProvider(), form.getTelegramBotId());

        List<AgentDefinitionProperties> definitions = new ArrayList<>(configurationGateway.loadAgentDefinitions());
        for (int i = 0; i < definitions.size(); i++) {
            AgentDefinitionProperties existing = definitions.get(i);
            if (!normalizedAgentId.equals(normalize(existing.getId()))) {
                continue;
            }

            AgentDefinitionProperties updated = toProperties(form, normalizedAgentId, existing);
            definitions.set(i, updated);
            configurationGateway.saveAgentDefinitions(definitions);
            return Optional.of(toViewItem(AgentDefinition.fromProperties(updated), accountStateByAgentId()));
        }

        return Optional.empty();
    }

    private List<AgentAdminPageView.AgentItem> toViewItems(List<AgentDefinition> definitions) {
        Map<String, AgentAccountAdminService.AgentAccountSummary> accountStateByAgentId = accountStateByAgentId();
        return definitions.stream()
                .map(definition -> toViewItem(definition, accountStateByAgentId))
                .sorted(Comparator.comparing(AgentAdminPageView.AgentItem::agentId, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AgentAdminPageView.AgentItem toViewItem(
            AgentDefinition definition,
            Map<String, AgentAccountAdminService.AgentAccountSummary> accountStateByAgentId
    ) {
        AgentAccountAdminService.AgentAccountSummary account = accountStateByAgentId.get(definition.id());
        return new AgentAdminPageView.AgentItem(
                definition.id(),
                definition.name(),
                definition.orchestratorType().name(),
                definition.role().name(),
                definition.llmProvider(),
                definition.model(),
                definition.telegramBotId(),
                definition.policyName(),
                definition.toolsEnabled(),
                definition.systemPrompt(),
                account == null ? null : account.enabled(),
                account == null ? null : account.createdAt()
        );
    }

    private Map<String, AgentAccountAdminService.AgentAccountSummary> accountStateByAgentId() {
        return agentAccountAdminService.listAgentAccounts().stream()
                .collect(Collectors.toMap(
                        AgentAccountAdminService.AgentAccountSummary::agentId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private AgentDefinitionProperties toProperties(
            AgentDefinitionAdminForm form,
            String agentId,
            AgentDefinitionProperties existing
    ) {
        String orchestratorType = requireEnum(form.getOrchestratorType(), AgentOrchestratorType.class, "orchestratorType");
        String role = requireEnum(form.getRole(), AgentRole.class, "role");

        AgentDefinitionProperties properties = new AgentDefinitionProperties();
        properties.setId(agentId);
        properties.setName(requireNonBlank(form.getName(), "name"));
        properties.setTelegramBotId(requireNonBlank(form.getTelegramBotId(), "telegramBotId"));
        properties.setOrchestratorType(orchestratorType);
        properties.setLlmProvider(requireNonBlank(form.getLlmProvider(), "llmProvider"));
        properties.setModel(requireNonBlank(form.getModel(), "model"));
        properties.setSystemPrompt(requireNonBlank(form.getSystemPrompt(), "systemPrompt"));
        properties.setRole(role);
        properties.setPolicyName(requireNonBlank(form.getPolicyName(), "policyName"));
        properties.setToolsEnabled(form.isToolsEnabled());
        // Keep policy details until dedicated policy editing is implemented in Marcel.
        properties.setPolicy(existing != null && existing.getPolicy() != null
                ? existing.getPolicy()
                : new AgentPolicyProperties());
        return properties;
    }

    private void ensureAssociationsExist(String llmProvider, String telegramBotId) {
        String normalizedLlmProvider = requireNonBlank(llmProvider, "llmProvider");
        String normalizedTelegramBotId = requireNonBlank(telegramBotId, "telegramBotId");

        boolean llmExists = listLlmOptions().stream()
                .anyMatch(llm -> normalizedLlmProvider.equals(normalize(llm.llmId())));
        if (!llmExists) {
            throw new AgentAdminValidationException("Unknown LLM provider: " + normalizedLlmProvider);
        }

        boolean botExists = listTelegramBotOptions().stream()
                .anyMatch(bot -> normalizedTelegramBotId.equals(normalize(bot.botId())));
        if (!botExists) {
            throw new AgentAdminValidationException("Unknown Telegram bot: " + normalizedTelegramBotId);
        }
    }

    private String requireNonBlank(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new AgentAdminValidationException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private <E extends Enum<E>> String requireEnum(String value, Class<E> enumType, String fieldName) {
        String normalized = requireNonBlank(value, fieldName).toUpperCase(Locale.ROOT);
        try {
            Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException exception) {
            throw new AgentAdminValidationException("Invalid " + fieldName + ": " + value);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
