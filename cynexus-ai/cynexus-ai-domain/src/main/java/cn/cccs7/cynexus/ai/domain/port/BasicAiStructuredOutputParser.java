package cn.cccs7.cynexus.ai.domain.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Framework-neutral baseline parser used by domain policies that do not need
 * infrastructure-specific schema features. Infrastructure may provide a richer
 * implementation through dependency injection.
 */
public class BasicAiStructuredOutputParser implements AiStructuredOutputParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String extractObject(String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("AI structured output is empty");
        if (content.length() > 256_000) throw new IllegalArgumentException("AI structured output exceeds size limit");
        String value = content.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        try {
            JsonNode parsed = mapper.readTree(value);
            if (parsed != null && (parsed.isObject() || parsed.isArray())) return parsed.toString();
        } catch (Exception ignored) {
            // Providers sometimes prefix JSON with a short explanation.
        }
        int objectStart = value.indexOf('{');
        int arrayStart = value.indexOf('[');
        int start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
        int objectEnd = value.lastIndexOf('}');
        int arrayEnd = value.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (start < 0 || end <= start) throw new IllegalArgumentException("AI structured output is invalid JSON");
        try {
            JsonNode parsed = mapper.readTree(value.substring(start, end + 1));
            if (parsed == null || (!parsed.isObject() && !parsed.isArray())) {
                throw new IllegalArgumentException("AI structured output must be JSON");
            }
            return parsed.toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI structured output is invalid JSON", exception);
        }
    }

    @Override
    public void validateSchema(String content, String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return;
        final JsonNode value;
        final JsonNode schema;
        try {
            value = mapper.readTree(extractObject(content));
            schema = mapper.readTree(schemaJson);
        } catch (Exception exception) {
            throw new IllegalArgumentException("structured output schema validation failed", exception);
        }
        validateNode(value, schema, "$", 0);
    }

    @Override
    public String sanitize(String content, Set<String> allowedFields, int maxStringLength, int maxArrayItems) {
        final JsonNode value;
        try { value = mapper.readTree(extractObject(content)); }
        catch (Exception exception) { throw new IllegalArgumentException("AI structured output is invalid JSON", exception); }
        return sanitizeNode(value, allowedFields == null ? Set.of() : allowedFields,
                Math.max(1, maxStringLength), Math.max(1, maxArrayItems), true).toString();
    }

    private void validateNode(JsonNode value, JsonNode schema, String path, int depth) {
        if (schema == null || schema.isNull() || depth > 32) return;
        String type = schema.path("type").asText("");
        if ("object".equals(type) && !value.isObject()) fail(path, "object required");
        if ("array".equals(type) && !value.isArray()) fail(path, "array required");
        if ("string".equals(type) && !value.isTextual()) fail(path, "string required");
        if (("number".equals(type) || "integer".equals(type)) && !value.isNumber()) fail(path, "number required");
        if ("boolean".equals(type) && !value.isBoolean()) fail(path, "boolean required");
        JsonNode enumValues = schema.get("enum");
        if (enumValues != null && enumValues.isArray() && !containsValue(enumValues, value)) fail(path, "value is not allowed");
        if (value.isTextual() && schema.has("maxLength") && value.textValue().length() > schema.path("maxLength").asInt()) fail(path, "string exceeds maxLength");
        if (value.isArray()) {
            if (schema.has("maxItems") && value.size() > schema.path("maxItems").asInt()) fail(path, "array exceeds maxItems");
            JsonNode items = schema.get("items");
            if (items != null) for (int i = 0; i < value.size(); i++) validateNode(value.get(i), items, path + "[" + i + "]", depth + 1);
        }
        if (value.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null && required.isArray()) for (JsonNode field : required) if (field.isTextual() && !value.has(field.textValue())) fail(path, "missing required field: " + field.textValue());
            JsonNode properties = schema.get("properties");
            if (properties != null && properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
                while (fields.hasNext()) { Map.Entry<String, JsonNode> field = fields.next(); JsonNode propertySchema = properties.get(field.getKey()); if (propertySchema != null) validateNode(field.getValue(), propertySchema, path + "." + field.getKey(), depth + 1); }
            }
        }
    }

    private JsonNode sanitizeNode(JsonNode value, Set<String> allowedFields, int maxStringLength, int maxArrayItems, boolean root) {
        if (value == null) return mapper.nullNode();
        if (value.isTextual()) return mapper.getNodeFactory().textNode(value.textValue().substring(0, Math.min(value.textValue().length(), maxStringLength)));
        if (value.isArray()) { ArrayNode result = mapper.createArrayNode(); for (int i = 0; i < Math.min(value.size(), maxArrayItems); i++) result.add(sanitizeNode(value.get(i), Set.of(), maxStringLength, maxArrayItems, false)); return result; }
        if (value.isObject()) { ObjectNode result = mapper.createObjectNode(); Iterator<Map.Entry<String, JsonNode>> fields = value.fields(); while (fields.hasNext()) { Map.Entry<String, JsonNode> field = fields.next(); if (root && !allowedFields.isEmpty() && !allowedFields.contains(field.getKey())) continue; result.set(field.getKey(), sanitizeNode(field.getValue(), Set.of(), maxStringLength, maxArrayItems, false)); } return result; }
        return value;
    }

    private boolean containsValue(JsonNode values, JsonNode value) { for (JsonNode allowed : values) if (allowed.equals(value)) return true; return false; }
    private void fail(String path, String message) { throw new IllegalArgumentException(path + ": " + message); }
}
