package cn.cccs7.cynexus.ai.domain.validation;

import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import cn.cccs7.cynexus.ai.domain.port.StructuredAiOutput;
import java.util.Objects;
import java.util.function.Function;

/**
 * Framework-neutral, single-attempt repair policy for structured model output.
 * The caller owns the capability/prompt context and supplies the repair call;
 * this policy only bounds parsing and preserves both failure reasons.
 */
public final class BoundedStructuredOutputRepairPolicy {
    private BoundedStructuredOutputRepairPolicy() { }

    public static RepairResult parseOrRepair(String raw, String originalError,
                                             AiStructuredOutputParser parser, int maxCharacters,
                                             Function<String, String> repairCall) {
        Objects.requireNonNull(parser, "structured output parser is required");
        StructuredAiOutput first = parser.parseBounded(raw, maxCharacters);
        if (first.successful()) return new RepairResult(first.json(), false, null, null);
        if (repairCall == null) {
            throw new RepairFailedException(first.error(), null);
        }
        String repaired;
        try {
            repaired = repairCall.apply(raw == null ? "" : raw);
        } catch (RuntimeException failure) {
            throw new RepairFailedException(first.error(), message(failure));
        }
        StructuredAiOutput second = parser.parseBounded(repaired, maxCharacters);
        if (!second.successful()) throw new RepairFailedException(first.error(), second.error());
        return new RepairResult(second.json(), true, first.error(), originalError);
    }

    private static String message(Throwable error) {
        return error == null || error.getMessage() == null ? "repair call failed" : error.getMessage();
    }

    public record RepairResult(String json, boolean repaired, String originalParseError, String callerError) { }

    public static final class RepairFailedException extends IllegalArgumentException {
        private final String originalError;
        private final String repairError;
        public RepairFailedException(String originalError, String repairError) {
            super("structured output repair failed; original=" + safe(originalError)
                    + (repairError == null ? "" : "; repair=" + safe(repairError)));
            this.originalError = originalError;
            this.repairError = repairError;
        }
        public String originalError() { return originalError; }
        public String repairError() { return repairError; }
        private static String safe(String value) { return value == null ? "unknown" : value.substring(0, Math.min(500, value.length())); }
    }
}
