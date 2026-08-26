package cn.cccs7.cynexus.market.domain.factorresearch.statistics;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronologicalStudySplitPolicyTest {
    @Test
    void keepsEverySameDayCohortOnOneSideOfTheChronologicalHoldout() {
        ChronologicalStudySplitPolicy.Split split = new ChronologicalStudySplitPolicy().split(List.of(
                value("c", "2026-08-03"), value("a", "2026-08-01"), value("b", "2026-08-02"), value("d", "2026-08-03"), value("e", "2026-08-04")));
        assertEquals(4, split.train().size());
        assertEquals(1, split.validation().size());
        assertTrue(split.train().stream().allMatch(value -> value.eventTradeDate().isBefore(split.validation().getFirst().eventTradeDate())));
    }

    private ChronologicalStudySplitPolicy.Observation value(String id, String date) {
        return new ChronologicalStudySplitPolicy.Observation(id, LocalDate.parse(date));
    }
}
