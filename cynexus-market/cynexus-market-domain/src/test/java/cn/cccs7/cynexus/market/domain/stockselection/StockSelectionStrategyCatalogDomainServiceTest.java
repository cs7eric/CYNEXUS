package cn.cccs7.cynexus.market.domain.stockselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StockSelectionStrategyCatalogDomainServiceTest {

    @Test
    void returnsAnImmutableActiveStrategyCatalog() {
        StockSelectionStrategyCatalogBO.Strategy strategy = new StockSelectionStrategyCatalogBO.Strategy(
                "TREND_MOMENTUM", "趋势动量", "test", "SCREENER_PRESET",
                new StockSelectionStrategyCatalogBO.Version(1L, 1, "{}", "[]", "initial", LocalDate.of(2026, 8, 15)));
        StockSelectionStrategyCatalogDomainService service = new StockSelectionStrategyCatalogDomainService(() -> List.of(strategy));

        StockSelectionStrategyCatalogBO.Catalog catalog = service.listActive();

        assertEquals(1, catalog.items().size());
        assertEquals("TREND_MOMENTUM", catalog.items().getFirst().code());
        assertTrue(catalog.items() != null);
    }

    @Test
    void normalizesNullRepositoryResultsToAnEmptyCatalog() {
        StockSelectionStrategyCatalogDomainService service = new StockSelectionStrategyCatalogDomainService(() -> null);

        assertTrue(service.listActive().items().isEmpty());
    }

    @Test
    void createsOwnerScopedImmutableUserStrategyAndAppendsVersionWithOptimisticLock() {
        InMemoryCatalogRepository repository = new InMemoryCatalogRepository();
        StockSelectionStrategyCatalogDomainService service = new StockSelectionStrategyCatalogDomainService(repository,
                new StockSelectionStrategyParameterPolicy(), new SequenceIdGenerator());
        StockSelectionStrategyRunBO.Scope owner = new StockSelectionStrategyRunBO.Scope("tenant-a", "owner-a");
        StockSelectionStrategyCatalogBO.SaveCommand create = new StockSelectionStrategyCatalogBO.SaveCommand("Volume breakout", "test",
                Map.of("universe", "ALL", "excludeSt", true, "volumeContractionDays", 4, "minBreakoutVolumeMultiple", 2.5,
                        "minBullishBodyPct", 4, "sort", "amount", "sortDir", "desc"), "Initial definition", null);

        StockSelectionStrategyCatalogBO.Strategy created = service.createUserStrategy(owner, create);
        StockSelectionStrategyCatalogBO.Strategy updated = service.updateUserStrategy(owner, created.code(),
                new StockSelectionStrategyCatalogBO.SaveCommand("Volume breakout v2", "refined", Map.of("minReturnPct10d", 3,
                        "minVolumeRatio", 1.2, "sort", "return_10d", "sortDir", "desc"), "Tightened momentum confirmation", 0L));

        assertEquals("USER_SCREENER", created.type());
        assertEquals(1, created.activeVersion().versionNo());
        assertEquals(2, updated.activeVersion().versionNo());
        assertEquals(1, updated.activeVersion().definitionVersion());
        assertEquals(1, service.listActive(owner).items().stream().filter(item -> item.code().equals(created.code())).count());
        assertTrue(service.listActive(new StockSelectionStrategyRunBO.Scope("tenant-b", "owner-b")).items().stream()
                .noneMatch(item -> item.code().equals(created.code())));
        assertThrows(IllegalStateException.class, () -> service.updateUserStrategy(owner, created.code(),
                new StockSelectionStrategyCatalogBO.SaveCommand("Stale", "", Map.of("minReturnPct10d", 2), "stale", 0L)));
    }

    @Test
    void rejectsUnknownOrEmptyUserStrategyCriteria() {
        StockSelectionStrategyCatalogDomainService service = new StockSelectionStrategyCatalogDomainService(new InMemoryCatalogRepository(),
                new StockSelectionStrategyParameterPolicy(), new SequenceIdGenerator());
        StockSelectionStrategyRunBO.Scope scope = new StockSelectionStrategyRunBO.Scope("tenant", "owner");

        assertThrows(IllegalArgumentException.class, () -> service.createUserStrategy(scope,
                new StockSelectionStrategyCatalogBO.SaveCommand("Unsafe", "", Map.of("rawSql", "select 1"), "initial", null)));
        assertThrows(IllegalArgumentException.class, () -> service.createUserStrategy(scope,
                new StockSelectionStrategyCatalogBO.SaveCommand("Empty", "", Map.of("universe", "ALL", "excludeSt", true), "initial", null)));
    }

    private static final class InMemoryCatalogRepository implements StockSelectionStrategyCatalogRepository {
        private final List<Entry> values = new ArrayList<>();
        @Override public List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies() { return List.of(); }
        @Override public List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies(StockSelectionStrategyRunBO.Scope scope) {
            return values.stream().filter(value -> value.scope.equals(scope)).map(Entry::strategy).toList();
        }
        @Override public boolean createUserStrategy(StockSelectionStrategyRunBO.Scope scope, StockSelectionStrategyCatalogBO.Strategy strategy) {
            values.add(new Entry(scope, strategy)); return true;
        }
        @Override public boolean appendUserStrategyVersion(StockSelectionStrategyRunBO.Scope scope, StockSelectionStrategyCatalogBO.Strategy strategy, long expectedDefinitionVersion) {
            for (int index = 0; index < values.size(); index++) {
                Entry current = values.get(index);
                if (current.scope.equals(scope) && current.strategy.code().equals(strategy.code())
                        && current.strategy.activeVersion().definitionVersion() == expectedDefinitionVersion) {
                    values.set(index, new Entry(scope, strategy)); return true;
                }
            }
            return false;
        }
        private record Entry(StockSelectionStrategyRunBO.Scope scope, StockSelectionStrategyCatalogBO.Strategy strategy) {}
    }

    private static final class SequenceIdGenerator implements StockSelectionIdGenerator {
        private final AtomicLong value = new AtomicLong(100);
        @Override public long nextId() { return value.incrementAndGet(); }
    }
}
