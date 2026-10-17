package cn.cccs7.cynexus.ai.infra.ai.structured;

import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationOutputParserPort;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for the knowledge-generation output contract.
 * Generic JSON extraction/bounds stay in the shared parser; field meaning is
 * mapped to the domain document contract here, before the domain draft policy
 * applies its business limits.
 */
@Component
public final class KnowledgeBaseGenerationOutputParserAdapter
        implements KnowledgeBaseGenerationOutputParserPort {

    private static final String FORMAT = """
            Output one JSON object only:
            {
              "title": "string",
              "description": "string",
              "tags": ["string"],
              "categories": ["string"],
              "content": "string"
            }
            """;

    private final AiStructuredOutputParser structuredOutputParser;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseGenerationOutputParserAdapter(AiStructuredOutputParser structuredOutputParser,
                                                       ObjectMapper objectMapper) {
        this.structuredOutputParser = structuredOutputParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public String formatInstructions() {
        return FORMAT;
    }

    @Override
    public KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception {
        String json = structuredOutputParser.extractObject(rawOutput);
        JsonNode root = objectMapper.readTree(json);
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Knowledge document output must be a JSON object");
        }
        return new KnowledgeBaseGenerationParsedDocument(
                requiredText(root, "title"),
                requiredText(root, "description"),
                stringList(root, "tags"),
                stringList(root, "categories"),
                requiredText(root, "content"));
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("Knowledge document field must be a string: " + field);
        }
        return value.textValue();
    }

    private List<String> stringList(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("Knowledge document field must be an array: " + field);
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Knowledge document array item must be a string: " + field);
            }
            result.add(item.textValue());
        }
        return result;
    }
}
