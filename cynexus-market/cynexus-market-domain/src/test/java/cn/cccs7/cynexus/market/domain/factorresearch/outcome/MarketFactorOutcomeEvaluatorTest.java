package cn.cccs7.cynexus.market.domain.factorresearch.outcome;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketFactorOutcomeEvaluatorTest {
    @Test
    void computesReturnExcessAndPathDependentMfeMaeWithoutFillingMissingHorizons() {
        MarketFactorOutcomeEvaluator evaluator = new MarketFactorOutcomeEvaluator();
        List<MarketFactorOutcomeEvaluator.Outcome> outcomes = evaluator.evaluate(new BigDecimal("10"), new BigDecimal("100"), List.of(
                close("2026-08-15", "11", "101"), close("2026-08-18", "9", "99")), List.of(1, 2, 3));
        assertEquals("10.000000", outcomes.get(0).stockReturnPct().toString());
        assertEquals("-9.000000", outcomes.get(1).excessReturnPct().toString());
        assertEquals("10.000000", outcomes.get(1).maximumFavorablePct().toString());
        assertEquals("-10.000000", outcomes.get(1).maximumAdversePct().toString());
        assertEquals("UNAVAILABLE", outcomes.get(2).status());
    }
    private MarketFactorOutcomeEvaluator.DailyClose close(String date, String close, String benchmark) {
        return new MarketFactorOutcomeEvaluator.DailyClose(LocalDate.parse(date), new BigDecimal(close), new BigDecimal(benchmark));
    }
}
