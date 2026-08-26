package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Materialized close-to-close daily moves; parameter range is part of the frozen study config. */
public final class LargeMoveCloseCohortProvider implements MarketEventCohortProvider {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private final MarketEventCohortDataRepository repository;
    public LargeMoveCloseCohortProvider(MarketEventCohortDataRepository repository) { this.repository = repository; }
    @Override public MarketFactorResearchBO.EventType eventType() { return MarketFactorResearchBO.EventType.LARGE_MOVE_CLOSE; }
    @Override public int definitionVersion() { return 1; }

    @Override public MarketEventCohort resolve(MarketEventCohortQuery query) {
        if (query.eventType() != eventType() || query.definitionVersion() != definitionVersion()) {
            throw new IllegalArgumentException("LARGE_MOVE_CLOSE provider received an incompatible query");
        }
        BigDecimal minimum = decimal(query.eventParams().get("minChangePct"));
        BigDecimal maximum = decimal(query.eventParams().get("maxChangePct"));
        List<MarketEventCohort.Sample> samples = repository.listLargeMoveCloses(query.startDate(), query.endDate(), minimum, maximum, query.maxSamples()).stream()
                .filter(value -> value.code() != null && !value.code().isBlank())
                .map(value -> new MarketEventCohort.Sample(value.code(), value.market(), value.tradeDate(),
                        value.tradeDate().atTime(LocalTime.of(15, 0)).atZone(MARKET_ZONE).toOffsetDateTime(), value.source(),
                        facts(value.changePct(), minimum, maximum)))
                .toList();
        boolean truncated = samples.size() == query.maxSamples();
        return new MarketEventCohort(eventType(), definitionVersion(), query.startDate(), query.endDate(), OffsetDateTime.now(MARKET_ZONE), samples,
                Map.of("source", "postgres.trade_review_security_daily_fact", "sampleCount", samples.size(), "complete", !truncated),
                truncated ? List.of("cohort was truncated at maxSamples") : List.of());
    }

    private BigDecimal decimal(Object value) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("large move range is required");
        return BigDecimal.valueOf(number.doubleValue());
    }

    private Map<String, Object> facts(BigDecimal changePct, BigDecimal minimum, BigDecimal maximum) {
        Map<String, Object> facts = new LinkedHashMap<>();
        if (changePct != null) facts.put("changePct", changePct);
        facts.put("minChangePct", minimum); facts.put("maxChangePct", maximum);
        return facts;
    }
}
