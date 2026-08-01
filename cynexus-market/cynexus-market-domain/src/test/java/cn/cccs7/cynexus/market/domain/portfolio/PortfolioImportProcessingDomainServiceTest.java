package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortfolioImportProcessingDomainServiceTest {
    @Test
    void validatesAndStoresCandidatesBeforeReadyForReview() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.PROCESSING, 1, 1);
        RecordingTasks tasks = new RecordingTasks();
        RecordingCandidates candidates = new RecordingCandidates();
        PortfolioImportProcessingDomainService service = new PortfolioImportProcessingDomainService(
                new ObjectMapper(), tasks, candidates);

        service.complete(task, "{\"isTradeScreenshot\":true,\"candidates\":[{\"rowNo\":1,\"symbol\":\"600000\",\"side\":\"BUY\",\"confidence\":0.9,\"warnings\":[]}]}", OffsetDateTime.now());

        assertEquals(1, candidates.values.size());
        assertEquals("600000", candidates.values.getFirst().normalizedPayload().path("symbol").asText());
        assertEquals(PortfolioImportStatus.READY_FOR_REVIEW, task.getStatus());
        assertSame(task, tasks.updated);
    }

    private static final class RecordingTasks implements PortfolioImportTaskRepository {
        private PortfolioImportTaskBO updated;
        @Override public void save(PortfolioImportTaskBO task) { }
        @Override public void update(PortfolioImportTaskBO task) { updated = task; }
        @Override public Optional<PortfolioImportTaskBO> findByOwnerAndId(String p, String t, long id) { return Optional.empty(); }
        @Override public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String p, String t, String k) { return Optional.empty(); }
    }

    private static final class RecordingCandidates implements PortfolioImportCandidateRepository {
        private final List<PortfolioImportCandidateBO> values = new ArrayList<>();
        @Override public void replace(long taskId, List<PortfolioImportCandidateBO> candidates) { values.addAll(candidates); }
    }
}
