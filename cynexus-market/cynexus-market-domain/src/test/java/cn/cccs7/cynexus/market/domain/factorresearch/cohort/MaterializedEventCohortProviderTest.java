package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterializedEventCohortProviderTest {
    private final LocalDate date = LocalDate.of(2026, 8, 14);
    private final MarketEventCohortDataRepository repository = new MarketEventCohortDataRepository() {
        @Override public List<IntradayEventFact> listLimitUpTouches(LocalDate start, LocalDate end, int limit) {
            return List.of(new IntradayEventFact("000001", "SZ", date, OffsetDateTime.parse("2026-08-14T10:15:00+08:00"),
                    false, new BigDecimal("9.5"), "materialized"));
        }
        @Override public List<DailyMoveFact> listLargeMoveCloses(LocalDate start, LocalDate end, BigDecimal min, BigDecimal max, int limit) {
            assertEquals(new BigDecimal("7.0"), min); assertEquals(new BigDecimal("12.0"), max);
            return List.of(new DailyMoveFact("300001", "SZ", date, new BigDecimal("8.1"), "materialized"));
        }
    };

    @Test
    void preservesFirstLimitUpAnchorForTouches() {
        MarketEventCohort cohort = new LimitUpTouchCohortProvider(repository).resolve(
                new MarketEventCohortQuery(MarketFactorResearchBO.EventType.LIMIT_UP_TOUCH, 1, date, date, 10));
        assertEquals("10:15", cohort.samples().getFirst().anchorAt().toLocalTime().toString());
        assertEquals(false, cohort.samples().getFirst().sourceFacts().get("finalLimitUp"));
    }

    @Test
    void freezesConfiguredLargeMoveRangeWithTheSample() {
        MarketEventCohort cohort = new LargeMoveCloseCohortProvider(repository).resolve(new MarketEventCohortQuery(
                MarketFactorResearchBO.EventType.LARGE_MOVE_CLOSE, 1, date, date, 10,
                Map.of("minChangePct", 7.0d, "maxChangePct", 12.0d)));
        assertEquals("8.1", cohort.samples().getFirst().sourceFacts().get("changePct").toString());
        assertEquals("7.0", cohort.samples().getFirst().sourceFacts().get("minChangePct").toString());
    }
}
