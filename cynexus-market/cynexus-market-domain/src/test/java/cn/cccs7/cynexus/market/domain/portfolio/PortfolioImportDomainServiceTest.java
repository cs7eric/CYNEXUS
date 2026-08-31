package cn.cccs7.cynexus.market.domain.portfolio;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void boundsImportPageRequestsBeforeQueryingTheRepository() {
        dependencies.tasks.put("first", new PortfolioImportTaskBO(1L, "user", "tenant", 1L, "first",
                PortfolioImportStatus.FAILED, 0, 1));

        PortfolioImportTaskPageBO page = service.list("user", "tenant", 0, 100);

        assertEquals(1, page.page());
        assertEquals(50, page.pageSize());
        assertEquals(1, page.total());
    }

    @Test
    void retriesFailedTaskAndDispatchesItAgain() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "user", "tenant", 2L, "request-1",
                PortfolioImportStatus.PENDING_DISPATCH, 0, 0);
        task.claimForProcessing(OffsetDateTime.parse("2026-08-01T00:00:00Z"));
        task.markFailed("PORTFOLIO_AI_RESPONSE_INVALID", "invalid response",
                OffsetDateTime.parse("2026-08-01T00:01:00Z"));
        dependencies.tasks.put(task.getIdempotencyKey(), task);

        PortfolioImportTaskBO retried = service.retry("user", "tenant", task.getId());

        assertEquals(PortfolioImportStatus.PENDING_DISPATCH, retried.getStatus());
        assertEquals(1, dependencies.dispatchCount);
        assertNull(retried.getLastErrorMessage());
    }

    @Test
    void reprocessesTaskThatIsWaitingForManualReview() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "user", "tenant", 2L, "request-1",
                PortfolioImportStatus.PENDING_DISPATCH, 0, 0);
        task.claimForProcessing(OffsetDateTime.parse("2026-08-01T00:00:00Z"));
        task.markReadyForReview(OffsetDateTime.parse("2026-08-01T00:01:00Z"));
        dependencies.tasks.put(task.getIdempotencyKey(), task);

        PortfolioImportTaskBO reprocessed = service.retry("user", "tenant", task.getId());

        assertEquals(PortfolioImportStatus.PENDING_DISPATCH, reprocessed.getStatus());
        assertNull(reprocessed.getCompletedAt());
        assertEquals(1, dependencies.dispatchCount);
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
        public Optional<PortfolioImportAssetBO> findById(long assetId) {
            return Optional.ofNullable(assets.get(assetId));
        }

        @Override
        public void save(PortfolioImportTaskBO task) {
            tasks.put(task.getIdempotencyKey(), task);
        }

        @Override
        public void update(PortfolioImportTaskBO task) {
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
        public List<PortfolioImportTaskBO> findRecentByOwner(String principalKey, String tenantKey, int limit) {
            return tasks.values().stream().limit(limit).toList();
        }

        @Override
        public PortfolioImportTaskPageBO findPageByOwner(String principalKey, String tenantKey, int page, int pageSize) {
            List<PortfolioImportTaskBO> values = tasks.values().stream().skip((long) (page - 1) * pageSize).limit(pageSize).toList();
            return new PortfolioImportTaskPageBO(values, tasks.size(), page, pageSize);
        }

        @Override
        public void dispatch(long taskId, String principalKey, String tenantKey) {
            dispatchCount++;
        }
    }
}
