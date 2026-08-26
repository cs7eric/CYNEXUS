package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.time.LocalDate;
import java.util.Map;

/** Bounded historical event query. Range bounds prevent accidental full-history scans. */
public record MarketEventCohortQuery(MarketFactorResearchBO.EventType eventType, int definitionVersion,
                                     LocalDate startDate, LocalDate endDate, int maxSamples, Map<String, Object> eventParams) {
    public MarketEventCohortQuery {
        if (eventType == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("event type and an ordered date range are required");
        }
        if (definitionVersion < 1) throw new IllegalArgumentException("cohort definition version must be positive");
        if (maxSamples < 1 || maxSamples > 20_000) throw new IllegalArgumentException("maxSamples must be between 1 and 20000");
        if (startDate.plusDays(730).isBefore(endDate)) {
            throw new IllegalArgumentException("cohort date range must not exceed 730 calendar days");
        }
        eventParams = eventParams == null ? Map.of() : Map.copyOf(eventParams);
    }

    public MarketEventCohortQuery(MarketFactorResearchBO.EventType eventType, int definitionVersion,
                                  LocalDate startDate, LocalDate endDate, int maxSamples) {
        this(eventType, definitionVersion, startDate, endDate, maxSamples, Map.of());
    }
}
