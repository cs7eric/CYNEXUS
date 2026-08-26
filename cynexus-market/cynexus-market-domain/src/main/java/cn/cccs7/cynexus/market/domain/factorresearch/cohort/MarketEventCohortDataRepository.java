package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Read-only port over already materialized Trade Review daily event facts. */
public interface MarketEventCohortDataRepository {
    List<IntradayEventFact> listLimitUpTouches(LocalDate startDate, LocalDate endDate, int limit);
    List<DailyMoveFact> listLargeMoveCloses(LocalDate startDate, LocalDate endDate, BigDecimal minChangePct,
                                             BigDecimal maxChangePct, int limit);

    record IntradayEventFact(String code, String market, LocalDate tradeDate, OffsetDateTime firstLimitUpAt,
                             boolean finalLimitUp, BigDecimal changePct, String source) { }
    record DailyMoveFact(String code, String market, LocalDate tradeDate, BigDecimal changePct, String source) { }
}
