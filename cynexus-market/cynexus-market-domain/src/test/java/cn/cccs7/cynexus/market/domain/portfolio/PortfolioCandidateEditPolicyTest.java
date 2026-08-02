package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PortfolioCandidateEditPolicyTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizesAndValidatesEditableTradeFields() {
        var normalized = PortfolioCandidateEditPolicy.normalize(mapper, "600000", "Example", "sell",
                "2026-08-02", "100", "12.50", "1.25", "T-1");

        assertEquals("600000", normalized.path("symbol").asText());
        assertEquals("SELL", normalized.path("side").asText());
        assertEquals("100", normalized.path("quantity").asText());
        assertEquals("1.25", normalized.path("fee").asText());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "60000", null, "BUY", "2026-08-02", "100", "12.50", "0", null));
        assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "600000", null, "BUY", "2026-08-02", "0", "12.50", "0", null));
    }
}
