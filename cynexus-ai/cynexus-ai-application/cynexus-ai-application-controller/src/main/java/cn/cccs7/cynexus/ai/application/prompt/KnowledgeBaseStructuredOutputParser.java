package cn.cccs7.cynexus.ai.application.prompt;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseStructuredOutputParser {

    private static final String FORMAT = """
            输出 JSON Schema：
            {
              "title": "string",
              "description": "string",
              "tags": ["string"],
              "categories": ["string"],
              "content": "string"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getFormat() {
        return FORMAT;
    }

    public KnowledgeBaseGenerateDTO convert(String rawJson) throws IOException {
        JsonNode root = objectMapper.readTree(rawJson);
        validate(root);
        return KnowledgeBaseGenerateDTO.builder()
                .title(text(root, "title"))
                .description(text(root, "description"))
                .tags(list(root, "tags"))
                .categories(list(root, "categories"))
                .content(text(root, "content"))
                .build();
    }

    private void validate(JsonNode root) throws IOException {
        if (root == null || !root.isObject()) {
            throw new IOException("Structured output must be a JSON object");
        }
        requireText(root, "title");
        requireText(root, "description");
        requireArray(root, "tags");
        requireArray(root, "categories");
        requireText(root, "content");
    }

    private void requireText(JsonNode root, String field) throws IOException {
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new IOException("Structured output field must be a string: " + field);
        }
    }

    private void requireArray(JsonNode root, String field) throws IOException {
        JsonNode value = root.get(field);
        if (value == null || !value.isArray()) {
            throw new IOException("Structured output field must be an array: " + field);
        }
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IOException("Structured output array item must be a string: " + field);
            }
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private List<String> list(JsonNode root, String field) {
        JsonNode values = root.get(field);
        if (values == null || !values.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!value.isNull()) {
                result.add(value.asText(""));
            }
        });
        return result;
    }
}
