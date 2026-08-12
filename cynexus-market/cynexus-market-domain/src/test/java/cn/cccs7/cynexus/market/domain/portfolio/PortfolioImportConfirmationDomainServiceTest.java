package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortfolioImportConfirmationDomainServiceTest {
    @Test
    void confirmsVisionTimestampWithoutOffset() throws Exception {
        PortfolioImportTaskBO task = task();
        PortfolioCandidateRecord candidate = candidate();
        RecordingCandidates candidates = new RecordingCandidates(candidate);
        RecordingTrades trades = new RecordingTrades();
        RecordingTradeQueries queries = new RecordingTradeQueries();
        RecordingTasks tasks = new RecordingTasks();
        PortfolioImportConfirmationDomainService service = service(candidates, trades, queries, tasks);

        service.confirm(task, List.of(3L), "u", OffsetDateTime.parse("2026-08-02T00:00:00Z"),
                LocalDate.of(2026, 7, 31));

        assertEquals(OffsetDateTime.parse("2026-07-31T09:30:10+08:00"), trades.trade.tradedAt());
        assertEquals("0.1", trades.trade.totalFee().toPlainString());
    }

    @Test
    void marksExistingSymbolAndTradeTimeAsExcludedForReview() {
        PortfolioImportTaskBO task = task();
        PortfolioCandidateRecord candidate = candidate();
        RecordingTradeQueries queries = new RecordingTradeQueries(new PortfolioTradeBO(9L, "159796", "ETF",
                PortfolioInstrumentType.ETF, TradeSide.BUY, OffsetDateTime.parse("2026-07-31T09:30:10Z"),
                new BigDecimal("200"), new BigDecimal("0.84"), new BigDecimal("0.1"), TradeStatus.ACTIVE));
        PortfolioImportConfirmationDomainService service = service(new RecordingCandidates(candidate), new RecordingTrades(),
                queries, new RecordingTasks());

        PortfolioCandidateReview reviewed = service.candidatesForReview(task, List.of(candidate), LocalDate.of(2026, 7, 31)).getFirst();

        assertEquals("EXCLUDED", reviewed.candidate().reviewStatus());
        assertTrue(reviewed.candidate().warnings().stream().anyMatch(item -> item.startsWith("Duplicate trade:")));
    }

    @Test
    void skipsExistingDuplicateWithoutFailingConfirmation() {
        PortfolioImportTaskBO task = task();
        RecordingTrades trades = new RecordingTrades();
        RecordingTradeQueries queries = new RecordingTradeQueries(new PortfolioTradeBO(9L, "159796", "ETF",
                PortfolioInstrumentType.ETF, TradeSide.BUY, OffsetDateTime.parse("2026-07-31T09:30:10Z"),
                new BigDecimal("200"), new BigDecimal("0.84"), new BigDecimal("0.1"), TradeStatus.ACTIVE));
        PortfolioImportConfirmationDomainService service = service(new RecordingCandidates(candidate()), trades, queries,
                new RecordingTasks());

        service.confirm(task, List.of(3L), "u", OffsetDateTime.parse("2026-08-02T00:00:00Z"),
                LocalDate.of(2026, 7, 31));

        assertNull(trades.trade);
        assertEquals(PortfolioImportStatus.CONFIRMED, task.getStatus());
    }

    @Test
    void excludesAndDoesNotConfirmNonTodayTransactions() {
        PortfolioImportTaskBO task = task();
        RecordingCandidates candidates = new RecordingCandidates(candidate());
        RecordingTrades trades = new RecordingTrades();
        PortfolioImportConfirmationDomainService service = service(candidates, trades,
                new RecordingTradeQueries(), new RecordingTasks());

        PortfolioCandidateReview reviewed = service.candidatesForReview(task, List.of(candidate()),
                LocalDate.of(2026, 8, 1)).getFirst();
        assertEquals("EXCLUDED", reviewed.candidate().reviewStatus());
        assertTrue(reviewed.candidate().warnings().contains("Trade date is not today; only today's transactions are valid for import."));

        service.confirm(task, List.of(3L), "u", OffsetDateTime.parse("2026-08-02T00:00:00Z"),
                LocalDate.of(2026, 8, 1));
        assertNull(trades.trade);
        assertEquals(PortfolioImportStatus.CONFIRMED, task.getStatus());
    }

    @Test
    void suggestsAUniqueExactSymbolForCandidateWithoutCode() throws Exception {
        PortfolioCandidateRecord candidate = new PortfolioCandidateRecord(4L, 1L, 2,
                new ObjectMapper().readTree("{\"symbol\":null,\"securityName\":\"红星发展\","
                        + "\"instrumentType\":\"STOCK\",\"side\":\"BUY\","
                        + "\"tradedAt\":\"2026-07-31T09:30:10\",\"quantity\":\"100\",\"price\":\"10\"}"),
                "PENDING", 0, null, List.of());
        PortfolioImportConfirmationDomainService service = service(new RecordingCandidates(candidate), new RecordingTrades(),
                new RecordingTradeQueries(), new RecordingTasks(), (securityName, instrumentType) ->
                        "红星发展".equals(securityName) && instrumentType == PortfolioInstrumentType.STOCK
                                ? Optional.of("600367") : Optional.empty());

        PortfolioCandidateReview reviewed = service.candidatesForReview(task(), List.of(candidate),
                LocalDate.of(2026, 7, 31)).getFirst();

        assertEquals("600367", reviewed.suggestedSymbol());
        assertTrue(reviewed.candidate().normalizedPayload().path("symbol").isNull());
    }

    private static PortfolioImportConfirmationDomainService service(RecordingCandidates candidates,
                                                                     RecordingTrades trades,
                                                                     RecordingTradeQueries queries,
                                                                     RecordingTasks tasks) {
        return service(candidates, trades, queries, tasks, (securityName, instrumentType) -> Optional.empty());
    }

    private static PortfolioImportConfirmationDomainService service(RecordingCandidates candidates,
                                                                     RecordingTrades trades,
                                                                     RecordingTradeQueries queries,
                                                                     RecordingTasks tasks,
                                                                     PortfolioSecuritySymbolResolver resolver) {
        return new PortfolioImportConfirmationDomainService(candidates, trades, queries, tasks, resolver);
    }

    private static PortfolioImportTaskBO task() {
        return new PortfolioImportTaskBO(1L, "u", "t", 2L, "k",
                PortfolioImportStatus.READY_FOR_REVIEW, 1, 1);
    }

    private static PortfolioCandidateRecord candidate() {
        try {
            return new PortfolioCandidateRecord(3L, 1L, 1,
                    new ObjectMapper().readTree("{\"symbol\":\"159796\",\"instrumentType\":\"ETF\","
                            + "\"side\":\"BUY\",\"tradedAt\":\"2026-07-31T09:30:10\","
                            + "\"quantity\":\"200\",\"price\":\"0.84\",\"fee\":null}"),
                    "PENDING", 0, null, List.of());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
        @Override public boolean save(String principalKey, String tenantKey, long sourceTaskId, long sourceCandidateId,
                                      PortfolioTradeBO trade, String confirmedBy) { this.trade = trade; return true; }
    }

    private static final class RecordingTradeQueries implements PortfolioTradeQueryRepository {
        private final List<PortfolioTradeBO> trades;
        private RecordingTradeQueries(PortfolioTradeBO... trades) { this.trades = List.of(trades); }
        @Override public List<PortfolioTradeBO> list(String principalKey, String tenantKey, String symbol) {
            return trades.stream().filter(item -> item.symbol().equals(symbol)).toList();
        }
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
