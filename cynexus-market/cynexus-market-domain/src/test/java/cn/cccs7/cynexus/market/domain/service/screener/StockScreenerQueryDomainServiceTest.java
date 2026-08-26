package cn.cccs7.cynexus.market.domain.service.screener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.repository.StockScreenerRepository;
import cn.cccs7.cynexus.market.domain.service.screener.impl.StockScreenerQueryDomainServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockScreenerQueryDomainServiceTest {

    @Test
    void appliesUniverseStPriceAndSortCriteriaBeforePaging() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(
                row("600001", "SH", false, "10", "3", "500"),
                row("600002", "SH", true, "12", "8", "900"),
                row("000001", "SZ", false, "11", "5", "700")
        )));

        StockScreenerQueryBO.Result result = service.screen(query(
                "2026-08-14", "SH", true, new BigDecimal("9"), null, new BigDecimal("1"), null));

        assertTrue(result.ok());
        assertEquals(LocalDate.of(2026, 8, 14), result.tradeDate());
        assertEquals(2, result.universeCount());
        assertEquals(1, result.count());
        assertEquals("600001", result.items().getFirst().code());
        assertEquals("READY", result.dataQuality().status());
        assertEquals(100, result.dataQuality().fieldCoverage().getFirst().coveragePct().intValue());
    }

    @Test
    void keepsRowsWithMissingOptionalFieldsWhenNoRelatedCriterionIsSet() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(
                row("000001", "SZ", false, "10", null, "500")
        )));

        StockScreenerQueryBO.Result result = service.screen(query(null, "ALL", false, null, null, null, null));

        assertTrue(result.ok());
        assertEquals(1, result.count());
        assertFalse(result.items().isEmpty());
    }

    @Test
    void excludesChiNextAndStarByDefaultUnlessExplicitlyIncluded() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(
                row("600001", "SH", false, "10", "3", "500", "main_sh"),
                row("300001", "SZ", false, "10", "3", "500", "chinext"),
                row("688001", "SH", false, "10", "3", "500", "star")
        )));

        assertEquals(1, service.screen(query(null, "ALL", true, null, null, null, null)).count());
        assertEquals(3, service.screen(query(null, "ALL", true, null, null, null, null, null, false, false)).count());
    }

    @Test
    void rejectsInvertedNumericRanges() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot()));

        assertThrows(IllegalArgumentException.class, () -> service.screen(query(
                null, "ALL", true, new BigDecimal("20"), new BigDecimal("10"), null, null)));
    }

    @Test
    void appliesContractionBreakoutAndBullishBodyAsHardCriteria() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(
                row("000001", "SZ", false, "10", "6", "100000000"))));

        StockScreenerQueryBO.PriceVolumePattern matched = new StockScreenerQueryBO.PriceVolumePattern(3,
                new BigDecimal("3"), new BigDecimal("5"));
        StockScreenerQueryBO.PriceVolumePattern rejected = new StockScreenerQueryBO.PriceVolumePattern(4,
                new BigDecimal("3"), new BigDecimal("5"));

        assertEquals(1, service.screen(query(null, "ALL", true, null, null, null, null, matched)).count());
        assertEquals(0, service.screen(query(null, "ALL", true, null, null, null, null, rejected)).count());
    }

    @Test
    void rejectsAnAmbiguousBreakoutMultipleWithoutAContractionBaseline() {
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot()));

        assertThrows(IllegalArgumentException.class, () -> service.screen(query(null, "ALL", true, null, null, null, null,
                new StockScreenerQueryBO.PriceVolumePattern(null, new BigDecimal("3"), null))));
    }

    @Test
    void appliesTheExtendedTechnicalPatternAsHardCriteria() {
        StockScreenerQueryBO.PriceVolumePattern pattern = new StockScreenerQueryBO.PriceVolumePattern(
                null, null, null, null, new BigDecimal("2"), new BigDecimal("1"), 20,
                new BigDecimal("4"), 2, new BigDecimal("1.3"), null, null);
        StockScreenerQueryBO.PriceVolumePatternMetrics metrics = new StockScreenerQueryBO.PriceVolumePatternMetrics(
                3, decimal("3.2"), decimal("5.1"), decimal("3"), decimal("1.5"), 25,
                decimal("3.5"), 2, decimal("1.4"), List.of());
        StockScreenerQueryBO.Row row = new StockScreenerQueryBO.Row("000001", "Ping An", "SZ", "Technology", "main_sz", false,
                decimal("10"), decimal("6"), decimal("4"), decimal("100000000"), decimal("1000000000"), decimal("500000000"),
                decimal("6"), decimal("9"), decimal("12"), decimal("15"), decimal("20"), decimal("30000000"), decimal("80000000"),
                "TDX001", "Technology", "THS001", "Technology", List.of(), List.of(), decimal("3"), decimal("1.5"), decimal("20"), metrics);
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(row)));

        assertEquals(1, service.screen(query(null, "ALL", true, null, null, null, null, pattern)).count());
    }

    @Test
    void appliesVolumeRangesAndCandlestickPatternsAsHardCriteria() {
        StockScreenerQueryBO.PriceVolumePattern matched = new StockScreenerQueryBO.PriceVolumePattern(
                null, null, null, null, null, null, null, null, null,
                decimal("1.3"), decimal("1.5"), "doji");
        StockScreenerQueryBO.PriceVolumePattern rejected = new StockScreenerQueryBO.PriceVolumePattern(
                null, null, null, null, null, null, null, null, null,
                decimal("1.3"), decimal("1.3"), "doji");
        StockScreenerQueryBO.PriceVolumePatternMetrics metrics = new StockScreenerQueryBO.PriceVolumePatternMetrics(
                3, decimal("3.2"), decimal("5.1"), null, null, null, null, null, decimal("1.4"), List.of("doji"));
        StockScreenerQueryBO.Row row = new StockScreenerQueryBO.Row("000001", "Ping An", "SZ", "Technology", "main_sz", false,
                decimal("10"), decimal("6"), decimal("4"), decimal("100000000"), decimal("1000000000"), decimal("500000000"),
                decimal("6"), decimal("9"), decimal("12"), decimal("15"), decimal("20"), decimal("30000000"), decimal("80000000"),
                "TDX001", "Technology", "THS001", "Technology", List.of(), List.of(), decimal("3"), decimal("1.5"), decimal("20"), metrics);
        StockScreenerQueryDomainService service = new StockScreenerQueryDomainServiceImpl(repository(snapshot(row)));

        assertEquals(1, service.screen(query(null, "ALL", true, null, null, null, null, matched)).count());
        assertEquals(0, service.screen(query(null, "ALL", true, null, null, null, null, rejected)).count());
    }

    private StockScreenerRepository repository(StockScreenerQueryBO.Snapshot snapshot) {
        return new StockScreenerRepository() {
            @Override public Optional<StockScreenerQueryBO.Snapshot> findSnapshotOnOrBefore(LocalDate ignored,
                                                                                              StockScreenerQueryBO.PriceVolumePattern pattern,
                                                                                              int dailyHistorySessions) { return Optional.of(snapshot); }
            @Override public StockScreenerQueryBO.ClassificationOptions findClassificationOptionsOnOrBefore(LocalDate ignored) {
                return new StockScreenerQueryBO.ClassificationOptions(List.of(), List.of(), List.of(), List.of());
            }
        };
    }

    private StockScreenerQueryBO.Snapshot snapshot(StockScreenerQueryBO.Row... rows) {
        return new StockScreenerQueryBO.Snapshot(LocalDate.of(2026, 8, 14), List.of(rows), "test", rows.length);
    }

    private StockScreenerQueryBO.QueryInput query(String tradeDate,
                                                   String universe,
                                                   boolean excludeSt,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice,
                                                   BigDecimal minChangePct,
                                                   BigDecimal maxChangePct) {
        return query(tradeDate, universe, excludeSt, minPrice, maxPrice, minChangePct, maxChangePct, null);
    }

    private StockScreenerQueryBO.QueryInput query(String tradeDate,
                                                   String universe,
                                                   boolean excludeSt,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice,
                                                   BigDecimal minChangePct,
                                                   BigDecimal maxChangePct,
                                                   StockScreenerQueryBO.PriceVolumePattern pattern) {
        return query(tradeDate, universe, excludeSt, minPrice, maxPrice, minChangePct, maxChangePct, pattern, true, true);
    }

    private StockScreenerQueryBO.QueryInput query(String tradeDate,
                                                   String universe,
                                                   boolean excludeSt,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice,
                                                   BigDecimal minChangePct,
                                                   BigDecimal maxChangePct,
                                                   StockScreenerQueryBO.PriceVolumePattern pattern,
                                                   boolean excludeChiNext,
                                                   boolean excludeStar) {
        return new StockScreenerQueryBO.QueryInput(tradeDate, universe, excludeSt, excludeChiNext, excludeStar, minPrice, maxPrice,
                minChangePct, maxChangePct, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                "amount", "desc", 1, 20, null, null, pattern);
    }

    private StockScreenerQueryBO.Row row(String code, String market, boolean st, String price, String changePct, String amount) {
        return row(code, market, st, price, changePct, amount, "main_sh");
    }

    private StockScreenerQueryBO.Row row(String code, String market, boolean st, String price, String changePct, String amount, String board) {
        return new StockScreenerQueryBO.Row(code, code, market, "Technology", board, st,
                decimal(price), decimal(changePct), decimal("4"), decimal(amount), decimal("1000000000"), decimal("500000000"),
                decimal("6"), decimal("9"), decimal("12"), decimal("15"), decimal("20"), decimal("30000000"), decimal("80000000"),
                "TDX001", "Technology", "THS001", "Technology", List.of(), List.of(), decimal("3"), decimal("1.5"), decimal("20"),
                new StockScreenerQueryBO.PriceVolumePatternMetrics(3, decimal("3.2"), decimal("5.1")));
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
