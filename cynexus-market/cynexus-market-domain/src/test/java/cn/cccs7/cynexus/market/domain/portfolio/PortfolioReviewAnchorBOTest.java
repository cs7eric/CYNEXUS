package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PortfolioReviewAnchorBOTest {
    @Test
    void normalizesAnchorAndHumanTags() {
        PortfolioReviewAnchorBO anchor = new PortfolioReviewAnchorBO(1L, "user", "tenant", "600519", "stock", 8L, null,
                "trade", "buy", OffsetDateTime.parse("2026-08-07T10:00:00+08:00"), new BigDecimal("123.45"), "  突破  ", "放量,突破");

        assertEquals("600519", anchor.symbol());
        assertEquals("STOCK", anchor.instrumentType());
        assertEquals("TRADE", anchor.anchorType());
        assertEquals("[\"放量\",\"突破\"]", anchor.tags());
        assertEquals("突破", anchor.note());
    }

    @Test
    void requiresTradeIdForTradeAnchor() {
        assertThrows(IllegalArgumentException.class, () -> new PortfolioReviewAnchorBO(1L, "user", "tenant", "600519", "STOCK", null, null,
                "TRADE", "BUY", OffsetDateTime.now(), BigDecimal.ONE, null, null));
    }
}
