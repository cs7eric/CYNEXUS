package cn.cccs7.cynexus.market.domain.factorresearch;

import java.time.LocalDate;
import java.util.Objects;

/** Enforces point-in-time and promotion invariants before any study is persisted or screened. */
public final class MarketFactorResearchPolicy {

    public void validateConfig(MarketFactorResearchBO.StudyConfig config) {
        Objects.requireNonNull(config, "study config is required");
        Objects.requireNonNull(config.eventType(), "event type is required");
        if (config.startDate() == null || config.endDate() == null || config.startDate().isAfter(config.endDate())) {
            throw new IllegalArgumentException("study date range is invalid");
        }
        if (config.controlsPerEvent() < 1 || config.controlsPerEvent() > 5) {
            throw new IllegalArgumentException("controlsPerEvent must be between 1 and 5");
        }
        if (config.dailyPreWindowSessions() < 20 || config.dailyPreWindowSessions() > 252) {
            throw new IllegalArgumentException("dailyPreWindowSessions must be between 20 and 252");
        }
        if (config.minutePreWindowMinutes() < 0 || config.minutePreWindowMinutes() > 240) {
            throw new IllegalArgumentException("minutePreWindowMinutes must be between 0 and 240");
        }
        if (config.outcomeHorizons().isEmpty() || config.outcomeHorizons().stream().anyMatch(value -> value == null || value < 1 || value > 252)) {
            throw new IllegalArgumentException("outcome horizons must be between 1 and 252 sessions");
        }
        if (config.eventParams().size() > 16) throw new IllegalArgumentException("eventParams must contain at most 16 values");
        validateLargeMoveRange(config);
    }

    public void requireNoDailyLeakage(LocalDate featureTradeDate, LocalDate eventTradeDate) {
        if (featureTradeDate == null || eventTradeDate == null || !featureTradeDate.isBefore(eventTradeDate)) {
            throw new IllegalStateException("daily feature is not available before the event date");
        }
    }

    public void requireScreenable(MarketFactorResearchBO.DiscoveredFactorDefinition factor) {
        if (factor == null || factor.validationStatus() != MarketFactorResearchBO.DiscoveryStatus.PROMOTED || !factor.screenable()) {
            throw new IllegalStateException("only promoted discovered factors may be screened");
        }
    }

    private void validateLargeMoveRange(MarketFactorResearchBO.StudyConfig config) {
        if (config.eventType() != MarketFactorResearchBO.EventType.LARGE_MOVE_CLOSE) return;
        Double minimum = number(config.eventParams().get("minChangePct"));
        Double maximum = number(config.eventParams().get("maxChangePct"));
        if (minimum == null || maximum == null || minimum < 0 || maximum > 30 || minimum > maximum) {
            throw new IllegalArgumentException("LARGE_MOVE_CLOSE requires minChangePct and maxChangePct in [0, 30]");
        }
    }

    private Double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        return null;
    }
}
