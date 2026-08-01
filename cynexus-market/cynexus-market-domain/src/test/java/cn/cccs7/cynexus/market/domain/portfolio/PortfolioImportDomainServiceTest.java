package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioImportDomainServiceTest {
    private final RecordingDependencies dependencies = new RecordingDependencies();
    private final PortfolioImportDomainService service = new PortfolioImportDomainService(
            () -> dependencies.nextId++, dependencies, dependencies, dependencies);

    @Test
    void createsAssetTaskAndDispatchesWithOneIdempotencyKey() {
        PortfolioImportTaskBO task = service.create("user", "tenant", "trade.png", "image/png",
                new byte[]{1, 2, 3}, "request-1", OffsetDateTime.parse("2026-08-01T00:00:00Z"));

        assertEquals(2L, task.getId());
        assertEquals(1, dependencies.assets.size());
        assertEquals(1, dependencies.dispatchCount);
        assertEquals(task.getId(), dependencies.tasks.get("request-1").getId());
    }

    @Test
    void returnsExistingTaskForRepeatedIdempotencyKey() {
        PortfolioImportTaskBO first = service.create("user", "tenant", "trade.png", "image/png",
                new byte[]{1}, "request-1", OffsetDateTime.now());
        PortfolioImportTaskBO second = service.create("user", "tenant", "trade.png", "image/png",
                new byte[]{9}, "request-1", OffsetDateTime.now());

        assertEquals(first.getId(), second.getId());
        assertEquals(1, dependencies.assets.size());
        assertEquals(1, dependencies.dispatchCount);
    }

    @Test
    void rejectsUnsupportedImageType() {
        assertThrows(IllegalArgumentException.class, () -> service.create("user", "tenant", "trade.pdf",
                "application/pdf", new byte[]{1}, "request-1", OffsetDateTime.now()));
    }

    private static final class RecordingDependencies implements PortfolioImportAssetStorePort,
            PortfolioImportTaskRepository, PortfolioImportDispatchPort {
        private long nextId = 1;
        private final Map<String, PortfolioImportTaskBO> tasks = new HashMap<>();
        private final Map<Long, PortfolioImportAssetBO> assets = new HashMap<>();
        private int dispatchCount;

        @Override
        public long save(PortfolioImportAssetBO asset) {
            assets.put(asset.id(), asset);
            return asset.id();
        }

        @Override
        public void save(PortfolioImportTaskBO task) {
            tasks.put(task.getIdempotencyKey(), task);
        }

        @Override
        public Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId) {
            return tasks.values().stream().filter(task -> task.getId() == taskId).findFirst();
        }

        @Override
        public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey) {
            return Optional.ofNullable(tasks.get(idempotencyKey));
        }

        @Override
        public void dispatch(long taskId, String principalKey, String tenantKey) {
            dispatchCount++;
        }
    }
}
