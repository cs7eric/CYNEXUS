package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PortfolioCandidateEditPolicyTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizesAndValidatesEditableTradeFields() {
        var normalized = PortfolioCandidateEditPolicy.normalize(mapper, "600000", "Example", "stock", "sell",
                "2026-08-02", "100", "12.50", "1.25", "T-1");

        assertEquals("600000", normalized.path("symbol").asText());
        assertEquals("SELL", normalized.path("side").asText());
        assertEquals("STOCK", normalized.path("instrumentType").asText());
        assertEquals("100", normalized.path("quantity").asText());
        assertEquals("1.25", normalized.path("fee").asText());
    }

    @Test
    void acceptsLocalDateTimeWithoutOffsetFromVisionImport() {
        var normalized = PortfolioCandidateEditPolicy.normalize(mapper, "159796", "电池", "ETF", "BUY",
                "2026-07-31T09:30:10", "200", "0.84", "0", null);

        assertEquals("2026-07-31T09:30:10+08:00", normalized.path("tradedAt").asText());
        assertEquals("0.1", normalized.path("fee").asText());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "60000", null, "ETF", "BUY", "2026-08-02", "100", "12.50", "0", null));
        assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "600000", null, "ETF", "BUY", "2026-08-02", "0", "12.50", "0", null));
        assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "159629", null, "FUND", "BUY", "2026-08-02", "100", "2.99", "0", null));
    }

    @Test
    void rejectsTimeOnlyTradeTimestampWithActionableMessage() {
        var exception = assertThrows(IllegalArgumentException.class, () -> PortfolioCandidateEditPolicy.normalize(mapper,
                "600000", null, "STOCK", "BUY", "10:41:17", "100", "12.50", "0", null));

        assertEquals("tradedAt must include a date; time-only values cannot be confirmed", exception.getMessage());
    }
}
