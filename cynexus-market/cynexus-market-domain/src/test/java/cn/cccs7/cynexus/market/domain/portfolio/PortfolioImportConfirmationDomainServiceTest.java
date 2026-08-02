package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortfolioImportConfirmationDomainServiceTest {
    @Test
    void confirmsVisionTimestampWithoutOffset() throws Exception {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.READY_FOR_REVIEW, 1, 1);
        PortfolioCandidateRecord candidate = new PortfolioCandidateRecord(3L, 1L, 1,
                new ObjectMapper().readTree("{\"symbol\":\"159796\",\"instrumentType\":\"ETF\","
                        + "\"side\":\"BUY\",\"tradedAt\":\"2026-07-31T09:30:10\","
                        + "\"quantity\":\"200\",\"price\":\"0.84\",\"fee\":null}"),
                "PENDING", 0, null, List.of());
        RecordingCandidates candidates = new RecordingCandidates(candidate);
        RecordingTrades trades = new RecordingTrades();
        RecordingTasks tasks = new RecordingTasks();
        PortfolioImportConfirmationDomainService service = new PortfolioImportConfirmationDomainService(
                candidates, trades, tasks);

        service.confirm(task, List.of(3L), "u", OffsetDateTime.parse("2026-08-02T00:00:00Z"));

        assertEquals(OffsetDateTime.parse("2026-07-31T09:30:10Z"), trades.trade.tradedAt());
    }

    private static final class RecordingCandidates implements PortfolioCandidateStore {
        private final PortfolioCandidateRecord candidate;
        private RecordingCandidates(PortfolioCandidateRecord candidate) { this.candidate = candidate; }
        @Override public List<PortfolioCandidateRecord> listByTaskId(long taskId) { return List.of(candidate); }
        @Override public boolean updateNormalized(long taskId, long candidateId, long expectedVersion, String normalizedPayload) { return false; }
        @Override public boolean markAccepted(long candidateId, long expectedVersion) { return true; }
    }

    private static final class RecordingTrades implements PortfolioTradeRepository {
        private PortfolioTradeBO trade;
        @Override public void save(String principalKey, String tenantKey, long sourceTaskId, long sourceCandidateId,
                                   PortfolioTradeBO trade, String confirmedBy) { this.trade = trade; }
    }

    private static final class RecordingTasks implements PortfolioImportTaskRepository {
        @Override public void save(PortfolioImportTaskBO task) { }
        @Override public void update(PortfolioImportTaskBO task) { }
        @Override public Optional<PortfolioImportTaskBO> findByOwnerAndId(String principalKey, String tenantKey, long taskId) { return Optional.empty(); }
        @Override public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String principalKey, String tenantKey, String idempotencyKey) { return Optional.empty(); }
        @Override public List<PortfolioImportTaskBO> findRecentByOwner(String principalKey, String tenantKey, int limit) { return List.of(); }
        @Override public PortfolioImportTaskPageBO findPageByOwner(String principalKey, String tenantKey, int page, int pageSize) {
            return new PortfolioImportTaskPageBO(List.of(), 0, page, pageSize);
        }
    }
}
