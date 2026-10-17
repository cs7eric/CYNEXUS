package cn.cccs7.cynexus.ai.domain.port;

import java.util.Set;

/** Framework-neutral boundary for extracting and validating structured model output. */
public interface AiStructuredOutputParser {
    AiStructuredOutputParser COMPATIBILITY_INSTANCE = new BasicAiStructuredOutputParser();

    /**
     * Compatibility accessor for legacy constructors and tests. Production
     * wiring injects the infrastructure parser so provider-specific schema
     * support remains in infra.
     */
    @Deprecated
    static AiStructuredOutputParser shared() {
        return COMPATIBILITY_INSTANCE;
    }

    String extractObject(String content);

    /** Validates the extracted JSON against the bounded schema subset stored with a Prompt. */
    default void validateSchema(String content, String schemaJson) {
        // Implementations may support the schema dialect used by the provider/runtime.
    }

    /** Sanitize a JSON object using framework-neutral bounds after extraction. */
    default String sanitize(String content, Set<String> allowedFields, int maxStringLength, int maxArrayItems) {
        return extractObject(content);
    }

    /** Parses provider output once; implementations must reject empty or oversized payloads. */
    default StructuredAiOutput parseBounded(String content, int maxCharacters) {
        if (content == null || content.length() > Math.max(1, maxCharacters)) {
            return new StructuredAiOutput(null, false, "structured output exceeds size limit");
        }
        return parse(content);
    }

    default StructuredAiOutput parse(String content) {
        try {
            return new StructuredAiOutput(extractObject(content), false, null);
        } catch (RuntimeException exception) {
            return new StructuredAiOutput(null, false,
                    exception.getMessage() == null ? "invalid structured output" : exception.getMessage());
        }
    }
}
