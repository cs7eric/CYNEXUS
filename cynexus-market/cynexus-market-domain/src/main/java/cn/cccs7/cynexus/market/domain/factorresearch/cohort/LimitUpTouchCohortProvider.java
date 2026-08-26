package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Uses historical first-touch facts; close is used only when the materialized anchor is absent. */
public final class LimitUpTouchCohortProvider implements MarketEventCohortProvider {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private final MarketEventCohortDataRepository repository;
    public LimitUpTouchCohortProvider(MarketEventCohortDataRepository repository) { this.repository = repository; }
    @Override public MarketFactorResearchBO.EventType eventType() { return MarketFactorResearchBO.EventType.LIMIT_UP_TOUCH; }
    @Override public int definitionVersion() { return 1; }

    @Override public MarketEventCohort resolve(MarketEventCohortQuery query) {
        if (query.eventType() != eventType() || query.definitionVersion() != definitionVersion()) {
            throw new IllegalArgumentException("LIMIT_UP_TOUCH provider received an incompatible query");
        }
        List<MarketEventCohort.Sample> samples = repository.listLimitUpTouches(query.startDate(), query.endDate(), query.maxSamples()).stream()
                .filter(value -> value.code() != null && !value.code().isBlank())
                .map(value -> new MarketEventCohort.Sample(value.code(), value.market(), value.tradeDate(),
                        value.firstLimitUpAt() == null ? value.tradeDate().atTime(LocalTime.of(15, 0)).atZone(MARKET_ZONE).toOffsetDateTime() : value.firstLimitUpAt(),
                        value.source(), facts(value.finalLimitUp(), value.changePct())))
                .toList();
        boolean truncated = samples.size() == query.maxSamples();
        return new MarketEventCohort(eventType(), definitionVersion(), query.startDate(), query.endDate(), OffsetDateTime.now(MARKET_ZONE), samples,
                Map.of("source", "postgres.trade_review_security_daily_fact", "sampleCount", samples.size(), "complete", !truncated),
                truncated ? List.of("cohort was truncated at maxSamples") : List.of());
    }

    private Map<String, Object> facts(boolean finalLimitUp, Object changePct) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("finalLimitUp", finalLimitUp);
        if (changePct != null) facts.put("changePct", changePct);
        return facts;
    }
}
