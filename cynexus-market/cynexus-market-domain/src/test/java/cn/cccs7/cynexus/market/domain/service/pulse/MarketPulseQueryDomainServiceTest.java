package cn.cccs7.cynexus.market.domain.service.pulse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MarketPulseQueryDomainServiceTest {

    @Test
    void fallsBackToLatestAvailableTradeDateAndSelectsIntradayMode() {
        LocalDate requested = LocalDate.now();
        LocalDate available = requested.minusDays(1);
        MarketPulseQueryRepository repository = repository(Map.of(
                "findLatestPulseTradeDate:" + requested, Optional.empty(),
                "findLatestPulseTradeDate:null", Optional.of(available),
                "findLatestPulseBatch", Optional.of(MarketPulseCaptureBatchBO.builder()
                        .tradeDate(available)
                        .sourceKind("live_capture")
                        .build()),
                "listPulseRowsByTradeDate", List.of()));

        MarketPulseQueryDomainService.SectorSnapshotResult result =
                new MarketPulseQueryDomainService(repository).latestSectorSnapshot(requested);

        assertEquals(available, result.tradeDate());
        assertTrue(result.fallbackTradeDate());
        assertEquals("close", result.servingMode());
    }

    @Test
    void servesLiveCaptureAsIntradayWhenTheTradeDateIsToday() {
        LocalDate today = LocalDate.now();
        MarketPulseQueryRepository repository = repository(Map.of(
                "findLatestPulseTradeDate:" + today, Optional.of(today),
                "findLatestPulseBatch", Optional.of(MarketPulseCaptureBatchBO.builder()
                        .tradeDate(today)
                        .sourceKind("live_capture")
                        .build()),
                "listPulseRowsByTradeDate", List.of()));

        MarketPulseQueryDomainService.SectorSnapshotResult result =
                new MarketPulseQueryDomainService(repository).latestSectorSnapshot(today);

        assertEquals("intraday", result.servingMode());
        assertFalse(result.fallbackTradeDate());
    }

    @Test
    void routesStylesToStyleSnapshotsAndOtherKindsToPersistedHeatmap() {
        HeatmapSnapshotBO persisted = HeatmapSnapshotBO.builder().kind("industries").payload("{}").build();
        MarketPulseQueryRepository repository = repository(Map.of(
                "listLatestStyleSectors", List.of(),
                "findLatestHeatmap:industries", Optional.of(persisted)));
        MarketPulseQueryDomainService service = new MarketPulseQueryDomainService(repository);

        MarketPulseQueryDomainService.HeatmapQuery styles = service.queryHeatmap("styles");
        MarketPulseQueryDomainService.HeatmapQuery industries = service.queryHeatmap("industries");

        assertTrue(styles.isStyles());
        assertEquals(0, styles.styleRows().size());
        assertFalse(industries.isStyles());
        assertEquals(persisted, industries.snapshot());
    }

    @Test
    void reportsMissingManualFundFlowAfterWrite() {
        AtomicReference<MarketOverviewBO> stored = new AtomicReference<>();
        LocalDate date = LocalDate.of(2026, 7, 23);
        MarketPulseQueryRepository repository = repository(Map.of());
        MarketPulseQueryRepository decorated = (MarketPulseQueryRepository) Proxy.newProxyInstance(
                MarketPulseQueryRepository.class.getClassLoader(),
                new Class<?>[]{MarketPulseQueryRepository.class},
                (proxy, method, args) -> {
                    if ("upsertManualFundFlow".equals(method.getName())) {
                        stored.set((MarketOverviewBO) args[1]);
                        return null;
                    }
                    if ("findManualFundFlow".equals(method.getName())) {
                        return Optional.ofNullable(null);
                    }
                    return invokeDefault(repository, method, args);
                });

        MarketPulseQueryDomainService.ManualFundFlowSaveResult result =
                new MarketPulseQueryDomainService(decorated).saveManualFundFlow(date, MarketOverviewBO.builder().build());

        assertTrue(stored.get() != null);
        assertFalse(result.foundAfterSave());
        assertEquals(date, result.tradingDate());
    }

    private MarketPulseQueryRepository repository(Map<String, Object> responses) {
        return (MarketPulseQueryRepository) Proxy.newProxyInstance(
                MarketPulseQueryRepository.class.getClassLoader(),
                new Class<?>[]{MarketPulseQueryRepository.class},
                (proxy, method, args) -> {
                    String key = method.getName();
                    if ("findLatestPulseTradeDate".equals(key)) {
                        key += ":" + (args[0] == null ? "null" : args[0]);
                    } else if ("findLatestHeatmap".equals(key)) {
                        key += ":" + args[0];
                    }
                    if (responses.containsKey(key)) {
                        return responses.get(key);
                    }
                    return invokeDefault(null, method, args);
                });
    }

    private Object invokeDefault(MarketPulseQueryRepository unused, java.lang.reflect.Method method, Object[] args) {
        if (method.getReturnType().equals(Optional.class)) {
            return Optional.empty();
        }
        if (method.getReturnType().equals(List.class)) {
            return List.of();
        }
        if (method.getReturnType().equals(void.class)) {
            return null;
        }
        return null;
    }
}
