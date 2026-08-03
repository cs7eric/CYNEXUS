package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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

        service.complete(task, "{\"isTradeScreenshot\":true,\"candidates\":[{\"rowNo\":1,\"symbol\":\"600000\",\"instrumentType\":\"STOCK\",\"side\":\"BUY\",\"confidence\":0.9,\"warnings\":[]}]}", OffsetDateTime.now());

        assertEquals(1, candidates.values.size());
        assertEquals("600000", candidates.values.getFirst().normalizedPayload().path("symbol").asText());
        assertEquals("STOCK", candidates.values.getFirst().normalizedPayload().path("instrumentType").asText());
        assertEquals(PortfolioImportStatus.READY_FOR_REVIEW, task.getStatus());
        assertSame(task, tasks.updated);
    }

    @Test
    void keepsExtractedCandidatesForUncertainTradeClassification() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.PROCESSING, 1, 1);
        RecordingTasks tasks = new RecordingTasks();
        RecordingCandidates candidates = new RecordingCandidates();
        PortfolioImportProcessingDomainService service = new PortfolioImportProcessingDomainService(
                new ObjectMapper(), tasks, candidates);

        service.complete(task, "{\"isTradeScreenshot\":false,\"candidates\":[{\"rowNo\":1,\"symbol\":\"159629\",\"securityName\":\"1000ETF\",\"instrumentType\":\"ETF\",\"side\":\"SELL\",\"confidence\":0.9,\"warnings\":[]}]}", OffsetDateTime.now());

        assertEquals(PortfolioImportStatus.READY_FOR_REVIEW, task.getStatus());
        assertTrue(candidates.values.getFirst().warnings().stream()
                .anyMatch(warning -> warning.contains("did not positively classify")));
        assertEquals("ETF", candidates.values.getFirst().normalizedPayload().path("instrumentType").asText());
    }

    @Test
    void normalizesVisionFieldEvidenceWrappersAndBrokerAliases() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.PROCESSING, 1, 1);
        RecordingTasks tasks = new RecordingTasks();
        RecordingCandidates candidates = new RecordingCandidates();
        PortfolioImportProcessingDomainService service = new PortfolioImportProcessingDomainService(
                new ObjectMapper(), tasks, candidates);

        service.complete(task, "{\"isTradeScreenshot\":true,\"candidates\":[{"
                + "\"securityCode\":{\"value\":\"159796\"},"
                + "\"instrumentType\":{\"value\":\"ETF\"},"
                + "\"side\":{\"value\":\"BUY\"},"
                + "\"tradeDate\":{\"value\":\"2026-07-31\"},"
                + "\"tradeTime\":{\"value\":\"11:24:58\"},"
                + "\"executionQuantity\":{\"value\":\"200\"},"
                + "\"executionPrice\":{\"value\":\"0.84\"},"
                + "\"securityName\":{\"value\":\"电池\"},"
                + "\"tradeId\":{\"value\":\"T-1\"}}]}", OffsetDateTime.now());

        JsonNode normalized = candidates.values.getFirst().normalizedPayload();
        assertEquals("159796", normalized.path("symbol").asText());
        assertEquals("ETF", normalized.path("instrumentType").asText());
        assertEquals("BUY", normalized.path("side").asText());
        assertEquals("2026-07-31T11:24:58", normalized.path("tradedAt").asText());
        assertEquals("200", normalized.path("quantity").asText());
        assertEquals("0.84", normalized.path("price").asText());
        assertEquals("电池", normalized.path("securityName").asText());
        assertEquals("T-1", normalized.path("externalTradeNo").asText());
    }

    @Test
    void normalizesCommonDateAndTimeAliasesWhenVisionUsesBrokerFieldNames() {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.PROCESSING, 1, 1);
        RecordingTasks tasks = new RecordingTasks();
        RecordingCandidates candidates = new RecordingCandidates();
        PortfolioImportProcessingDomainService service = new PortfolioImportProcessingDomainService(
                new ObjectMapper(), tasks, candidates);

        service.complete(task, "{\"isTradeScreenshot\":true,\"candidates\":[{"
                + "\"symbol\":\"600000\",\"side\":\"BUY\",\"date\":\"2026-07-31\","
                + "\"time\":\"09:30:10\",\"quantity\":\"100\",\"price\":\"10\"}]}", OffsetDateTime.now());

        assertEquals("2026-07-31T09:30:10", candidates.values.getFirst().normalizedPayload().path("tradedAt").asText());
    }

    private static final class RecordingTasks implements PortfolioImportTaskRepository {
        private PortfolioImportTaskBO updated;
        @Override public void save(PortfolioImportTaskBO task) { }
        @Override public void update(PortfolioImportTaskBO task) { updated = task; }
        @Override public Optional<PortfolioImportTaskBO> findByOwnerAndId(String p, String t, long id) { return Optional.empty(); }
        @Override public Optional<PortfolioImportTaskBO> findByIdempotencyKey(String p, String t, String k) { return Optional.empty(); }
        @Override public List<PortfolioImportTaskBO> findRecentByOwner(String p, String t, int limit) { return List.of(); }
        @Override public PortfolioImportTaskPageBO findPageByOwner(String p, String t, int page, int pageSize) { return new PortfolioImportTaskPageBO(List.of(), 0, page, pageSize); }
    }

    private static final class RecordingCandidates implements PortfolioImportCandidateRepository {
        private final List<PortfolioImportCandidateBO> values = new ArrayList<>();
        @Override public void replace(long taskId, List<PortfolioImportCandidateBO> candidates) { values.addAll(candidates); }
    }
}
