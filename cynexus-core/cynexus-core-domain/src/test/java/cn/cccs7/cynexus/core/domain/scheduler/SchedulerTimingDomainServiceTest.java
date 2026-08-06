package cn.cccs7.cynexus.core.domain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchedulerTimingDomainServiceTest {

    @Test
    void supportsSecondCadenceForFiveSecondMarketIndicatorRefresh() {
        SchedulerTimingDomainService service = new SchedulerTimingDomainService();
        OffsetDateTime now = OffsetDateTime.of(2026, 8, 5, 1, 0, 5, 0, ZoneOffset.UTC);
        assertThat(service.isDue(Map.of("every_seconds", 5), now.minusSeconds(5), now)).isTrue();
        assertThat(service.isDue(Map.of("every_seconds", 5), now.minusSeconds(4), now)).isFalse();
    }
}
