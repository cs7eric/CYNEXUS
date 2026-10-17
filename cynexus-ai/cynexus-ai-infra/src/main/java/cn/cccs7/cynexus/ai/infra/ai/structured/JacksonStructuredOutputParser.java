package cn.cccs7.cynexus.ai.infra.ai.structured;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.port.BasicAiStructuredOutputParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@Primary
public class JacksonStructuredOutputParser extends BasicAiStructuredOutputParser
        implements MediaTranscriptionStructuredOutputPort, ApplicationAnalysisStructuredOutputPort {
    private final ObjectMapper mapper = new ObjectMapper();

    /** Removes provider-added top-level fields when a capability supplies an allow-list. */
    public String extractObject(String content, Set<String> allowedFields) {
        try {
            JsonNode parsed = mapper.readTree(extractObject(content));
            if (parsed.isObject() && allowedFields != null && !allowedFields.isEmpty()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) parsed).retain(allowedFields);
            }
            return parsed.toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI structured output is invalid JSON", exception);
        }
    }

    @Override
    public String sanitize(String content, Set<String> allowedFields, int maxStringLength, int maxArrayItems) {
        try {
            JsonNode parsed = mapper.readTree(extractObject(content));
            return sanitizeNode(parsed, allowedFields, maxStringLength, maxArrayItems, true).toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI structured output is invalid JSON", exception);
        }
    }

    private JsonNode sanitizeNode(JsonNode node, Set<String> allowedFields, int maxStringLength,
                                  int maxArrayItems, boolean root) {
        if (node == null) return mapper.nullNode();
        if (node.isTextual() && maxStringLength > 0 && node.textValue().length() > maxStringLength) {
            return mapper.getNodeFactory().textNode(node.textValue().substring(0, maxStringLength));
        }
        if (node.isArray()) {
            var array = mapper.createArrayNode();
            int limit = maxArrayItems > 0 ? Math.min(maxArrayItems, node.size()) : node.size();
            for (int i = 0; i < limit; i++) array.add(sanitizeNode(node.get(i), null, maxStringLength, maxArrayItems, false));
            return array;
        }
        if (!node.isObject()) return node;
        var object = (com.fasterxml.jackson.databind.node.ObjectNode) node.deepCopy();
        if (root && allowedFields != null && !allowedFields.isEmpty()) object.retain(allowedFields);
        var names = new java.util.ArrayList<String>();
        object.fieldNames().forEachRemaining(names::add);
        names.forEach(name -> object.set(name,
                sanitizeNode(object.get(name), null, maxStringLength, maxArrayItems, false)));
        return object;
    }

    @Override
    public void validateSchema(String content, String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return;
        try {
            JsonNode schema = mapper.readTree(schemaJson);
            validateNode(mapper.readTree(extractObject(content)), schema, "$", 0);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI output schema is invalid", ex);
        }
    }

    private void validateNode(JsonNode value, JsonNode schema, String path, int depth) {
        if (depth > 16 || schema == null || schema.isMissingNode()) return;
        String type = schema.path("type").asText("");
        boolean valid = switch (type) {
            case "object" -> value != null && value.isObject();
            case "array" -> value != null && value.isArray();
            case "string" -> value != null && value.isTextual();
            case "number" -> value != null && value.isNumber();
            case "integer" -> value != null && value.isIntegralNumber();
            case "boolean" -> value != null && value.isBoolean();
            default -> true;
        };
        if (!valid) throw new IllegalArgumentException("AI output schema mismatch at " + path);
        JsonNode enums = schema.get("enum");
        if (enums != null && enums.isArray() && (value == null || !java.util.stream.StreamSupport.stream(enums.spliterator(), false).anyMatch(item -> item.equals(value)))) {
            throw new IllegalArgumentException("AI output enum mismatch at " + path);
        }
        if (value != null && value.isTextual()) {
            int maxLength = schema.path("maxLength").asInt(0);
            if (maxLength > 0 && value.textValue().length() > maxLength) {
                throw new IllegalArgumentException("AI output string exceeds maxLength at " + path);
            }
            int minLength = schema.path("minLength").asInt(0);
            if (minLength > 0 && value.textValue().length() < minLength) {
                throw new IllegalArgumentException("AI output string is shorter than minLength at " + path);
            }
        }
        if (value != null && value.isArray()) {
            int maxItems = schema.path("maxItems").asInt(0);
            if (maxItems > 0 && value.size() > maxItems) {
                throw new IllegalArgumentException("AI output array exceeds maxItems at " + path);
            }
            int minItems = schema.path("minItems").asInt(0);
            if (minItems > 0 && value.size() < minItems) {
                throw new IllegalArgumentException("AI output array is shorter than minItems at " + path);
            }
        }
        if (value != null && value.isObject()) {
            for (JsonNode required : schema.path("required")) if (!value.has(required.asText())) throw new IllegalArgumentException("AI output missing field " + path + "." + required.asText());
            JsonNode properties = schema.get("properties");
            if (properties != null) value.fields().forEachRemaining(entry -> { if (properties.has(entry.getKey())) validateNode(entry.getValue(), properties.get(entry.getKey()), path + "." + entry.getKey(), depth + 1); });
        } else if (value != null && value.isArray() && schema.has("items")) {
            value.elements().forEachRemaining(item -> validateNode(item, schema.get("items"), path + "[]", depth + 1));
        }
    }
}
