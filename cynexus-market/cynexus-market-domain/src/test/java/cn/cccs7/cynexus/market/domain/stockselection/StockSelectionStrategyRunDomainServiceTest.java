package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StockSelectionStrategyRunDomainServiceTest {

    @Test
    void persistsBoundedRunWithCandidateEvidenceAndNullableSnapshotFields() {
        StockSelectionStrategyCatalogBO.Strategy strategy = new StockSelectionStrategyCatalogBO.Strategy("TREND_MOMENTUM", "Trend", "test", "SCREENER_PRESET",
                new StockSelectionStrategyCatalogBO.Version(8L, 1, "{\"universe\":\"ALL\",\"excludeSt\":true,\"minReturnPct10d\":5,\"minReturnPct30d\":10,\"minTurnoverPct\":1,\"minVolumeRatio\":1,\"sort\":\"return_30d\",\"sortDir\":\"desc\"}", "[]", "initial", LocalDate.of(2026, 8, 15)));
        CapturingRepository repository = new CapturingRepository();
        StockSelectionStrategyRunDomainService service = new StockSelectionStrategyRunDomainService(
                new StockSelectionStrategyCatalogDomainService(() -> List.of(strategy)), screener(rowResult()), repository,
                new SequenceIdGenerator());

        StockSelectionStrategyRunBO.Run run = service.run(new StockSelectionStrategyRunBO.Scope("tenant-a", "owner-a"), "trend_momentum", "2026-08-15");

        assertEquals("SUCCEEDED", run.status());
        assertEquals("tenant-a", repository.saved.scope().tenantId());
        assertEquals(1, run.candidates().size());
        assertEquals("000001", run.candidates().getFirst().stockCode());
        assertTrue(run.candidates().getFirst().snapshot().containsKey("mainNet"));
        assertFalse(run.candidates().getFirst().evidence().isEmpty());
    }

    private StockScreenerQueryDomainService screener(StockScreenerQueryBO.Result result) {
        return new StockScreenerQueryDomainService() {
            @Override public StockScreenerQueryBO.Result screen(StockScreenerQueryBO.QueryInput ignored) { return result; }
            @Override public StockScreenerQueryBO.ClassificationOptions listClassificationOptions(String ignored) { return new StockScreenerQueryBO.ClassificationOptions(List.of(), List.of(), List.of(), List.of()); }
        };
    }

    private StockScreenerQueryBO.Result rowResult() {
        StockScreenerQueryBO.Row row = new StockScreenerQueryBO.Row("000001", "Test", "SZ", null, "main_sz", false,
                new BigDecimal("12"), new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("500000000"), new BigDecimal("1000000000"), null,
                new BigDecimal("3"), new BigDecimal("6"), new BigDecimal("15"), null, null, null, null,
                "TDX001", "Technology", "THS001", "Technology", List.of(), List.of(), new BigDecimal("2"), new BigDecimal("1.2"), null, null);
        return new StockScreenerQueryBO.Result(true, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15), "ALL", true, true, true,
                null, null, null, null, null, null, "return_30d", "desc", new StockScreenerQueryBO.Page(1, 100, 0),
                1, 1, "test", List.of(row), List.of(), null, null);
    }

    private static final class CapturingRepository implements StockSelectionStrategyRunRepository {
        private StockSelectionStrategyRunBO.Run saved;
        @Override public void save(StockSelectionStrategyRunBO.Run value) { saved = value; }
        @Override public java.util.Optional<StockSelectionStrategyRunBO.HealthInput> findLatestHealthInput(StockSelectionStrategyRunBO.Scope scope, String strategyCode) { return java.util.Optional.empty(); }
    }

    private static final class SequenceIdGenerator implements StockSelectionIdGenerator {
        private final AtomicLong sequence = new AtomicLong(100);
        @Override public long nextId() { return sequence.incrementAndGet(); }
    }
}
