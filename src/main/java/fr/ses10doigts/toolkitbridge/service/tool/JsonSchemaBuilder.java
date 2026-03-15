package fr.ses10doigts.toolkitbridge.service.tool;

import java.util.*;

public final class JsonSchemaBuilder {

    private final Map<String, Object> schema = new LinkedHashMap<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();

    private JsonSchemaBuilder() {
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
    }

    public static JsonSchemaBuilder object() {
        return new JsonSchemaBuilder();
    }

    public JsonSchemaBuilder stringProperty(String name, String description, boolean required) {
        properties.put(name, Map.of(
                "type", "string",
                "description", description
        ));
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public JsonSchemaBuilder stringArrayProperty(String name, String description, boolean required) {
        properties.put(name, Map.of(
                "type", "array",
                "description", description,
                "items", Map.of("type", "string")
        ));
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public JsonSchemaBuilder enumStringProperty(String name, String description, Set<String> values, boolean required) {
        properties.put(name, Map.of(
                "type", "string",
                "description", description,
                "enum", values
        ));
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public Map<String, Object> build() {
        return Map.copyOf(schema);
    }
}