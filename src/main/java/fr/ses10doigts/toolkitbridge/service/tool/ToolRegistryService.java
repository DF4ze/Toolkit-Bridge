package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ToolRegistryService {

    private final Map<String, ToolHandler> handlers;
    private final Map<String, ToolDescriptor> descriptors;

    public ToolRegistryService(List<ToolHandler> toolHandlers) {
        LinkedHashMap<String, ToolHandler> indexedHandlers = new LinkedHashMap<>();
        LinkedHashMap<String, ToolDescriptor> indexedDescriptors = new LinkedHashMap<>();

        for (ToolHandler handler : toolHandlers) {
            ToolDescriptor descriptor = handler.descriptor();
            String normalizedName = normalize(handler.name());

            if (!normalizedName.equals(descriptor.name())) {
                throw new IllegalStateException("Tool descriptor name mismatch for handler: " + handler.getClass().getName());
            }
            if (indexedHandlers.putIfAbsent(normalizedName, handler) != null) {
                throw new IllegalStateException("Duplicate tool handler registration: " + normalizedName);
            }
            if (indexedDescriptors.putIfAbsent(descriptor.name(), descriptor) != null) {
                throw new IllegalStateException("Duplicate tool descriptor registration: " + descriptor.name());
            }
        }

        this.handlers = Collections.unmodifiableMap(indexedHandlers);
        this.descriptors = Collections.unmodifiableMap(indexedDescriptors);
    }

    public List<ToolDefinition> getToolDefinitions() {
        return getToolDescriptors().stream()
                .map(ToolDescriptor::toToolDefinition)
                .toList();
    }

    public List<ToolDefinition> getToolDefinitions(Collection<String> toolNames) {
        return getToolDescriptors(toolNames).stream()
                .map(ToolDescriptor::toToolDefinition)
                .toList();
    }

    public ToolHandler getRequiredHandler(String name) {
        ToolHandler handler = handlers.get(normalize(name));
        if (handler == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return handler;
    }

    public List<ToolDescriptor> getToolDescriptors() {
        return descriptors.values().stream().toList();
    }

    public List<ToolDescriptor> getToolDescriptors(Collection<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }
        return toolNames.stream()
                .map(this::getDescriptor)
                .toList();
    }

    public ToolDescriptor getDescriptor(String toolName) {
        ToolDescriptor descriptor = descriptors.get(normalize(toolName));
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return descriptor;
    }

    public Set<String> getToolNames() {
        return Set.copyOf(descriptors.keySet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
