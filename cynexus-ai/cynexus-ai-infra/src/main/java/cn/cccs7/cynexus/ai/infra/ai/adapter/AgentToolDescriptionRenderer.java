package cn.cccs7.cynexus.ai.infra.ai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;

final class AgentToolDescriptionRenderer {
    private static final int MAX_DESCRIPTION_CHARS = 6_000;
    private static final int MAX_LIST_ITEMS = 6;
    private static final int MAX_EXAMPLES = 3;

    private AgentToolDescriptionRenderer() {
    }

    static String render(ObjectMapper objectMapper, String description, String guidanceJson) {
        String base = value(description);
        if (guidanceJson == null || guidanceJson.isBlank()) return base;
        try {
            JsonNode guidance = objectMapper.readTree(guidanceJson);
            if (guidance == null || !guidance.isObject()) return base;
            StringBuilder rendered = new StringBuilder(base);
            appendScalar(rendered, "Purpose", guidance.path("purpose"));
            appendList(rendered, "Use when", guidance.path("useWhen"), MAX_LIST_ITEMS);
            appendList(rendered, "Avoid when", guidance.path("avoidWhen"), MAX_LIST_ITEMS);
            appendObject(rendered, "Parameter notes", guidance.path("parameterNotes"), MAX_LIST_ITEMS);
            appendExamples(rendered, guidance.path("examples"), objectMapper);
            appendScalar(rendered, "Result notes", guidance.path("resultNotes"));
            return truncate(rendered.toString());
        } catch (JsonProcessingException ignored) {
            return base;
        }
    }

    private static void appendScalar(StringBuilder target, String label, JsonNode value) {
        if (value != null && value.isTextual() && !value.textValue().isBlank()) {
            target.append("\n").append(label).append(": ").append(value.textValue().trim());
        }
    }

    private static void appendList(StringBuilder target, String label, JsonNode values, int maxItems) {
        if (values == null || !values.isArray() || values.isEmpty()) return;
        target.append("\n").append(label).append(":");
        int count = 0;
        for (JsonNode value : values) {
            if (count++ >= maxItems) break;
            if (value.isTextual() && !value.textValue().isBlank()) {
                target.append("\n- ").append(value.textValue().trim());
            }
        }
    }

    private static void appendObject(StringBuilder target, String label, JsonNode values, int maxItems) {
        if (values == null || !values.isObject() || values.isEmpty()) return;
        target.append("\n").append(label).append(":");
        Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
        int count = 0;
        while (fields.hasNext() && count++ < maxItems) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isTextual() && !field.getValue().textValue().isBlank()) {
                target.append("\n- ").append(field.getKey()).append(": ")
                        .append(field.getValue().textValue().trim());
            }
        }
    }

    private static void appendExamples(StringBuilder target, JsonNode examples, ObjectMapper objectMapper) throws JsonProcessingException {
        if (examples == null || !examples.isArray() || examples.isEmpty()) return;
        target.append("\nExamples:");
        int count = 0;
        for (JsonNode example : examples) {
            if (count++ >= MAX_EXAMPLES || !example.isObject()) break;
            JsonNode arguments = example.get("arguments");
            String note = value(example.path("note").asText(null));
            if (arguments == null || !arguments.isObject()) continue;
            target.append("\n- ").append(objectMapper.writeValueAsString(arguments));
            if (!note.isBlank()) target.append(" (").append(note).append(")");
        }
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_DESCRIPTION_CHARS) return value;
        return value.substring(0, MAX_DESCRIPTION_CHARS - 3) + "...";
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
