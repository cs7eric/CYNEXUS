package cn.cccs7.cynexus.ai.domain.validation;

import static org.junit.jupiter.api.Assertions.*;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import org.junit.jupiter.api.Test;

class BoundedStructuredOutputRepairPolicyTest {
    private final AiStructuredOutputParser parser = new AiStructuredOutputParser() {
        @Override public String extractObject(String content) {
            if (content == null || !content.trim().startsWith("{")) throw new IllegalArgumentException("invalid json");
            return content.trim();
        }
    };

    @Test void performsAtMostOneRepairAndPreservesOriginalError() {
        var calls = new int[1];
        var result = BoundedStructuredOutputRepairPolicy.parseOrRepair("broken", "parse failed", parser, 100,
                ignored -> { calls[0]++; return "{\"ok\":true}"; });
        assertTrue(result.repaired());
        assertEquals(1, calls[0]);
        assertEquals("invalid json", result.originalParseError());
    }

    @Test void repairFailureIsExplicit() {
        var failure = assertThrows(BoundedStructuredOutputRepairPolicy.RepairFailedException.class,
                () -> BoundedStructuredOutputRepairPolicy.parseOrRepair("broken", "parse failed", parser, 100,
                        ignored -> "still broken"));
        assertTrue(failure.getMessage().contains("repair failed"));
        assertEquals("invalid json", failure.originalError());
    }
}
