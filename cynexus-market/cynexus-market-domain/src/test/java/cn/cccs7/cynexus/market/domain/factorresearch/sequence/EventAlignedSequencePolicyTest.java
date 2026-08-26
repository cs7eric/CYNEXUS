package cn.cccs7.cynexus.market.domain.factorresearch.sequence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventAlignedSequencePolicyTest {
    private final EventAlignedSequencePolicy policy = new EventAlignedSequencePolicy();

    @Test
    void alignsOnlyPreEventDailyPointsAndUsesRobustNormalization() {
        EventAlignedSequencePolicy.Sequence sequence = policy.daily("stock.amount", LocalDate.of(2026, 8, 14), List.of(
                point("2026-08-11", "10"), point("2026-08-12", "20"), point("2026-08-13", "30")));
        assertEquals(-3, sequence.points().getFirst().offset());
        assertEquals(-1, sequence.points().getLast().offset());
        assertEquals("0E-8", sequence.points().get(1).normalizedValue().toString());
    }

    @Test
    void rejectsEventDayDailyFeatureLeakage() {
        assertThrows(IllegalStateException.class, () -> policy.daily("stock.close", LocalDate.of(2026, 8, 14),
                List.of(point("2026-08-14", "10"))));
    }

    @Test
    void rejectsPrimitiveThatWasNotAvailableAtTheStudyCutoff() {
        assertThrows(IllegalStateException.class, () -> policy.daily("stock.close", LocalDate.of(2026, 8, 14),
                OffsetDateTime.parse("2026-08-13T15:00:00+08:00"), List.of(point("2026-08-13", "10"))));
    }

    private EventAlignedSequencePolicy.DailyPoint point(String date, String value) {
        return new EventAlignedSequencePolicy.DailyPoint(LocalDate.parse(date), OffsetDateTime.parse(date + "T16:00:00+08:00"), new BigDecimal(value));
    }
}
