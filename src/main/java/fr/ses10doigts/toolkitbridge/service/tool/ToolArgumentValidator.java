package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.exception.ToolValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ToolArgumentValidator {

    public void validate(Map<String, Object> schema, Map<String, Object> arguments) {
        if (schema == null) {
            throw new ToolValidationException("Schema cannot be null");
        }
        if (arguments == null) {
            throw new ToolValidationException("Arguments cannot be null");
        }

        String rootType = asString(schema.get("type"));
        if (!"object".equals(rootType)) {
            throw new ToolValidationException("Root schema type must be 'object'");
        }

        validateObject(schema, arguments, "arguments");
    }

    @SuppressWarnings("unchecked")
    private void validateObject(Map<String, Object> schema, Map<String, Object> value, String path) {
        Object propertiesObj = schema.get("properties");
        if (!(propertiesObj instanceof Map<?, ?> rawProperties)) {
            throw new ToolValidationException("Schema at '" + path + "' must define a properties map");
        }

        Map<String, Object> properties = (Map<String, Object>) rawProperties;

        Object requiredObj = schema.get("required");
        List<String> required = requiredObj instanceof List<?> rawRequired
                ? rawRequired.stream().map(String::valueOf).toList()
                : List.of();

        boolean additionalProperties = !Boolean.FALSE.equals(schema.get("additionalProperties"));

        // Vérification des champs requis
        for (String requiredField : required) {
            if (!value.containsKey(requiredField) || value.get(requiredField) == null) {
                throw new ToolValidationException("Missing required field: '" + path + "." + requiredField + "'");
            }
        }

        // Vérification des propriétés envoyées
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (!properties.containsKey(fieldName)) {
                if (!additionalProperties) {
                    throw new ToolValidationException("Unknown field: '" + path + "." + fieldName + "'");
                }
                continue;
            }

            Object fieldSchemaObj = properties.get(fieldName);
            if (!(fieldSchemaObj instanceof Map<?, ?> rawFieldSchema)) {
                throw new ToolValidationException("Invalid schema for field: '" + path + "." + fieldName + "'");
            }

            Map<String, Object> fieldSchema = (Map<String, Object>) rawFieldSchema;
            validateValue(fieldSchema, fieldValue, path + "." + fieldName);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateValue(Map<String, Object> schema, Object value, String path) {
        if (value == null) {
            return;
        }

        String type = asString(schema.get("type"));
        if (type == null || type.isBlank()) {
            throw new ToolValidationException("Missing schema type for '" + path + "'");
        }

        switch (type) {
            case "string" -> validateString(schema, value, path);
            case "integer" -> validateInteger(schema, value, path);
            case "number" -> validateNumber(schema, value, path);
            case "boolean" -> validateBoolean(schema, value, path);
            case "array" -> validateArray(schema, value, path);
            case "object" -> {
                if (!(value instanceof Map<?, ?> rawMap)) {
                    throw new ToolValidationException("Field '" + path + "' must be an object");
                }
                validateObject(schema, (Map<String, Object>) rawMap, path);
            }
            default -> throw new ToolValidationException("Unsupported schema type '" + type + "' for '" + path + "'");
        }
    }

    private void validateString(Map<String, Object> schema, Object value, String path) {
        if (!(value instanceof String stringValue)) {
            throw new ToolValidationException("Field '" + path + "' must be a string");
        }

        validateEnum(schema, stringValue, path);
    }

    private void validateInteger(Map<String, Object> schema, Object value, String path) {
        if (!(value instanceof Integer || value instanceof Long)) {
            throw new ToolValidationException("Field '" + path + "' must be an integer");
        }

        validateEnum(schema, value, path);
    }

    private void validateNumber(Map<String, Object> schema, Object value, String path) {
        if (!(value instanceof Number)) {
            throw new ToolValidationException("Field '" + path + "' must be a number");
        }

        validateEnum(schema, value, path);
    }

    private void validateBoolean(Map<String, Object> schema, Object value, String path) {
        if (!(value instanceof Boolean)) {
            throw new ToolValidationException("Field '" + path + "' must be a boolean");
        }

        validateEnum(schema, value, path);
    }

    @SuppressWarnings("unchecked")
    private void validateArray(Map<String, Object> schema, Object value, String path) {
        if (!(value instanceof List<?> listValue)) {
            throw new ToolValidationException("Field '" + path + "' must be an array");
        }

        validateEnum(schema, value, path);

        Object itemsObj = schema.get("items");
        if (itemsObj == null) {
            return;
        }

        if (!(itemsObj instanceof Map<?, ?> rawItemsSchema)) {
            throw new ToolValidationException("Field '" + path + "' has invalid items schema");
        }

        Map<String, Object> itemsSchema = (Map<String, Object>) rawItemsSchema;

        for (int i = 0; i < listValue.size(); i++) {
            validateValue(itemsSchema, listValue.get(i), path + "[" + i + "]");
        }
    }

    private void validateEnum(Map<String, Object> schema, Object value, String path) {
        Object enumObj = schema.get("enum");
        if (enumObj == null) {
            return;
        }

        if (!(enumObj instanceof List<?> allowedValues)) {
            throw new ToolValidationException("Invalid enum definition for '" + path + "'");
        }

        boolean match = allowedValues.stream().anyMatch(allowed -> Objects.equals(allowed, value));
        if (!match) {
            throw new ToolValidationException(
                    "Field '" + path + "' must be one of " + allowedValues + " but was: " + value
            );
        }
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}