package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** LIMIT_UP_CLOSE is backed by the persisted daily final limit-up pool, never by live market state. */
public final class LimitUpCloseCohortProvider implements MarketEventCohortProvider {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CATEGORY = "limit_up";
    private final MarketPulseQueryRepository repository;

    public LimitUpCloseCohortProvider(MarketPulseQueryRepository repository) { this.repository = repository; }

    @Override public MarketFactorResearchBO.EventType eventType() { return MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE; }
    @Override public int definitionVersion() { return 1; }

    @Override
    public MarketEventCohort resolve(MarketEventCohortQuery query) {
        if (query.eventType() != eventType() || query.definitionVersion() != definitionVersion()) {
            throw new IllegalArgumentException("LIMIT_UP_CLOSE provider received an incompatible query");
        }
        List<MarketEventCohort.Sample> samples = new ArrayList<>();
        int sourceRows = 0;
        for (LocalDate date = query.startDate(); !date.isAfter(query.endDate()) && samples.size() < query.maxSamples(); date = date.plusDays(1)) {
            List<MarketLimitDailyStockBO> rows = repository.listLimitStocks(date, CATEGORY,
                    Math.min(500, query.maxSamples() - samples.size()));
            sourceRows += rows.size();
            for (MarketLimitDailyStockBO row : rows) {
                if (row.getCode() == null || row.getCode().isBlank()) continue;
                OffsetDateTime anchorAt = date.atTime(LocalTime.of(15, 0)).atZone(MARKET_ZONE).toOffsetDateTime();
                Map<String, Object> facts = new LinkedHashMap<>();
                putIfPresent(facts, "category", row.getCategory());
                putIfPresent(facts, "streak", row.getStreak());
                putIfPresent(facts, "changePct", row.getChangePct());
                putIfPresent(facts, "limitUpPrice", row.getLimitUpPrice());
                samples.add(new MarketEventCohort.Sample(row.getCode().trim(), market(row.getCode()), date, anchorAt,
                        "postgres.market_limit_daily_stocks", facts));
                if (samples.size() == query.maxSamples()) break;
            }
        }
        Map<String, Object> coverage = Map.of("source", "postgres.market_limit_daily_stocks", "sourceRows", sourceRows,
                "sampleCount", samples.size(), "complete", samples.size() < query.maxSamples());
        List<String> warnings = samples.size() == query.maxSamples() ? List.of("cohort was truncated at maxSamples") : List.of();
        return new MarketEventCohort(eventType(), definitionVersion(), query.startDate(), query.endDate(),
                OffsetDateTime.now(MARKET_ZONE), samples, coverage, warnings);
    }

    private String market(String code) { return code != null && code.startsWith("6") ? "SH" : "SZ"; }

    private void putIfPresent(Map<String, Object> values, String key, Object value) {
        if (value != null) values.put(key, value);
    }
}
