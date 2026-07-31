package cn.cccs7.cynexus.market.domain.service.futures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;
import cn.cccs7.cynexus.market.domain.repository.FuturesPositionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FuturesPositionQueryDomainServiceTest {
    @Test
    void servesLatestPriorDateAndNormalizesFilters() {
        QueryArguments args = new QueryArguments();
        FuturesPositionRepository repository = new EmptyRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
                args.endDate = endDate;
                return Optional.of(LocalDate.of(2026, 7, 21));
            }
            @Override public List<FuturesPositionQueryBO.Instrument> listInstruments(LocalDate date, String scope,
                    String exchange, String variety, int top, String sort, String sortDir, int offset, int limit) {
                args.scope = scope;
                args.exchange = exchange;
                args.variety = variety;
                args.top = top;
                args.offset = offset;
                args.limit = limit;
                return List.of();
            }
        };

        FuturesPositionQueryBO.Latest value = new FuturesPositionQueryDomainServiceImpl(repository)
                .latest("2026-07-22", " variety ", " cffex ", " if ", 99, "bad", "asc", 2, 999);

        assertTrue(value.ok());
        assertTrue(value.fallbackTradeDate());
        assertEquals(LocalDate.of(2026, 7, 22), value.requestedTradeDate());
        assertEquals(LocalDate.of(2026, 7, 21), value.tradeDate());
        assertEquals("VARIETY", args.scope);
        assertEquals("CFFEX", args.exchange);
        assertEquals("IF", args.variety);
        assertEquals(10, args.top);
        assertEquals(100, args.offset);
        assertEquals(100, args.limit);
    }

    @Test
    void returnsTruthfulEmptyState() {
        FuturesPositionQueryBO.Latest value = new FuturesPositionQueryDomainServiceImpl(new EmptyRepository())
                .latest(null, null, null, null, 5, null, null, null, null);

        assertFalse(value.ok());
        assertEquals("futures position data is empty", value.error());
        assertTrue(value.instruments().isEmpty());
    }

    @Test
    void treatsAllExchangeSentinelAsUnfilteredQuery() {
        QueryArguments args = new QueryArguments();
        FuturesPositionRepository repository = new EmptyRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
                return Optional.of(LocalDate.of(2026, 7, 21));
            }

            @Override public List<FuturesPositionQueryBO.Instrument> listInstruments(LocalDate date, String scope,
                    String exchange, String variety, int top, String sort, String sortDir, int offset, int limit) {
                args.exchange = exchange;
                return List.of();
            }
        };

        new FuturesPositionQueryDomainServiceImpl(repository)
                .latest(null, "CONTRACT", "ALL", null, 10, null, null, null, null);

        assertNull(args.exchange);
    }

    @Test
    void rejectsUnsupportedMemberRankType() {
        FuturesPositionRepository repository = new EmptyRepository() {
            @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
                return Optional.of(LocalDate.of(2026, 7, 21));
            }
        };

        assertThrows(IllegalArgumentException.class, () -> new FuturesPositionQueryDomainServiceImpl(repository)
                .members(null, "CONTRACT", "CFFEX", "IF2609", "net", 10));
    }

    private static class QueryArguments {
        private LocalDate endDate;
        private String scope;
        private String exchange;
        private String variety;
        private int top;
        private int offset;
        private int limit;
    }

    private static class EmptyRepository implements FuturesPositionRepository {
        @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.empty(); }
        @Override public List<FuturesPositionQueryBO.Instrument> listInstruments(LocalDate date, String scope,
                String exchange, String variety, int top, String sort, String sortDir, int offset, int limit) { return List.of(); }
        @Override public int countInstruments(LocalDate date, String scope, String exchange, String variety) { return 0; }
        @Override public FuturesPositionQueryBO.Aggregate aggregate(LocalDate date, String scope, String exchange,
                String variety, int top) { return new FuturesPositionQueryBO.Aggregate(null, null, null, null, null, null, null); }
        @Override public List<FuturesPositionQueryBO.Member> listMembers(LocalDate date, String scope, String exchange,
                String instrument, String rankType, int top) { return List.of(); }
        @Override public List<FuturesPositionQueryBO.History> listHistory(String scope, String exchange,
                String instrument, int top, int limit) { return List.of(); }
        @Override public List<FuturesPositionQueryBO.CoverageItem> listCoverage(LocalDate date) { return List.of(); }
    }
}
