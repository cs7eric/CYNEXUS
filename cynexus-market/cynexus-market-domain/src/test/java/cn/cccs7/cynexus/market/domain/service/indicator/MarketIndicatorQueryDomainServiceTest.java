package cn.cccs7.cynexus.market.domain.service.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MarketIndicatorQueryDomainServiceTest {

    @Test
    void keepsGlobalReplayEventsBoundedToOneThousand() {
        AtomicReference<String> capturedIndustryCode = new AtomicReference<>();
        AtomicInteger capturedLimit = new AtomicInteger();
        MarketIndicatorQueryDomainService service = new MarketIndicatorQueryDomainService(
                capturingRepository(capturedIndustryCode, capturedLimit));

        service.replayTimeline(LocalDate.of(2026, 8, 7), LocalTime.of(9, 30), LocalTime.of(15, 0),
                null, null, 10_000);

        assertNull(capturedIndustryCode.get());
        assertEquals(1000, capturedLimit.get());
    }

    @Test
    void passesNormalizedIndustryCodeAndFullBoundedLimit() {
        AtomicReference<String> capturedIndustryCode = new AtomicReference<>();
        AtomicInteger capturedLimit = new AtomicInteger();
        MarketIndicatorQueryDomainService service = new MarketIndicatorQueryDomainService(
                capturingRepository(capturedIndustryCode, capturedLimit));

        service.replayTimeline(LocalDate.of(2026, 8, 7), LocalTime.of(9, 30), LocalTime.of(15, 0),
                null, " 881121 ", 10_000);

        assertEquals("881121", capturedIndustryCode.get());
        assertEquals(10_000, capturedLimit.get());
    }

    @Test
    void returnsCompactReadyMaterializationManifestWithoutReadingReplayRows() {
        MarketIndicatorRepository repository = (MarketIndicatorRepository) Proxy.newProxyInstance(
                MarketIndicatorRepository.class.getClassLoader(),
                new Class<?>[]{MarketIndicatorRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "tradeReviewMarketStateManifest" -> Map.of(
                            "status", "READY", "minute_bar_count", 1_700_000, "context_count", 7_000);
                    case "toString" -> "MarketIndicatorRepositoryTestProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        MarketIndicatorQueryDomainService service = new MarketIndicatorQueryDomainService(repository);

        Map<String, Object> result = service.tradeReviewMarketStateStatus(LocalDate.of(2026, 8, 7));

        assertEquals("ready", result.get("dataStatus"));
        assertEquals("ready", result.get("status"));
        assertEquals(Map.of("status", "READY", "minute_bar_count", 1_700_000, "context_count", 7_000), result.get("manifest"));
        assertEquals(List.of(), result.get("qualityReasons"));
        assertEquals(List.of(), result.get("missingReasons"));
    }

    private MarketIndicatorRepository capturingRepository(AtomicReference<String> industryCode,
                                                           AtomicInteger eventLimit) {
        return (MarketIndicatorRepository) Proxy.newProxyInstance(
                MarketIndicatorRepository.class.getClassLoader(),
                new Class<?>[]{MarketIndicatorRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "listReplayEvents" -> {
                        industryCode.set((String) args[4]);
                        eventLimit.set((Integer) args[5]);
                        yield List.of();
                    }
                    case "listReplayIndustries" -> List.of();
                    case "toString" -> "MarketIndicatorRepositoryTestProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
