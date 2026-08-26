package cn.cccs7.cynexus.market.domain.factorresearch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketFactorResearchPolicyTest {

    private final MarketFactorResearchPolicy policy = new MarketFactorResearchPolicy();

    @Test
    void rejectsEventDayDailyFeatureAsLeakage() {
        assertThrows(IllegalStateException.class,
                () -> policy.requireNoDailyLeakage(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 14)));
    }

    @Test
    void acceptsBoundedStudyConfig() {
        assertDoesNotThrow(() -> policy.validateConfig(new MarketFactorResearchBO.StudyConfig(
                MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 1),
                List.of(1, 3, 5, 10, 20), 4, 60, 120,
                List.of("CHANGE_POINT", "SEQUENCE_CLUSTER", "SEQUENTIAL_RULE", "INTERPRETABLE_SUBGROUP"))));
    }
}
