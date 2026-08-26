package cn.cccs7.cynexus.market.domain.screener;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class StockScreenerDomainPolicy {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;
    private static final java.util.Set<String> CANDLESTICK_PATTERNS = java.util.Set.of(
            "bullish_engulfing", "bearish_engulfing", "hammer", "shooting_star", "doji", "gap_up", "gap_down", "inside_bar");

    public StockScreenerQueryBO.Query normalize(StockScreenerQueryBO.QueryInput input) {
        if (input == null) {
            return new StockScreenerQueryBO.Query(null, "ALL", true, true, true, null, null, null, null,
                    null, null, null, null, null, "ALL", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, "amount", "desc",
                    new StockScreenerQueryBO.Page(1, DEFAULT_PAGE_SIZE, 0), null, null, null);
        }
        StockScreenerQueryBO.QueryInput value = input;
        BigDecimal minPrice = nonNegative(value.minPrice(), "minPrice");
        BigDecimal maxPrice = nonNegative(value.maxPrice(), "maxPrice");
        BigDecimal minAmount = nonNegative(value.minAmount(), "minAmount");
        BigDecimal minMarketCap = nonNegative(value.minMarketCap(), "minMarketCap");
        BigDecimal maxMarketCap = nonNegative(value.maxMarketCap(), "maxMarketCap");
        ensureRange(minPrice, maxPrice, "price");
        ensureRange(value.minChangePct(), value.maxChangePct(), "changePct");
        ensureRange(minMarketCap, maxMarketCap, "marketCap");
        ensureRange(value.minAmplitudePct(), value.maxAmplitudePct(), "amplitudePct");
        ensureRange(value.minReturnPct5d(), value.maxReturnPct5d(), "returnPct5d");
        ensureRange(value.minReturnPct10d(), value.maxReturnPct10d(), "returnPct10d");
        ensureRange(value.minReturnPct30d(), value.maxReturnPct30d(), "returnPct30d");
        ensureRange(value.minReturnPct60d(), value.maxReturnPct60d(), "returnPct60d");
        ensureRange(value.minReturnPct252d(), value.maxReturnPct252d(), "returnPct252d");
        ensureRange(value.minTurnoverPct(), value.maxTurnoverPct(), "turnoverPct");
        ensureRange(value.minVolumeRatio(), value.maxVolumeRatio(), "volumeRatio");
        ensureRange(value.minPe(), value.maxPe(), "pe");
        int pageNumber = value.page() == null || value.page() < 1 ? 1 : value.page();
        int pageSize = value.pageSize() == null || value.pageSize() < 1 ? DEFAULT_PAGE_SIZE
                : Math.min(value.pageSize(), MAX_PAGE_SIZE);
        String stockCode = normalizeStockCode(value.stockCode());
        String stockMarket = normalizeStockMarket(value.stockMarket());
        if ((stockCode == null) != (stockMarket == null)) {
            throw new IllegalArgumentException("stockCode and stockMarket must be provided together");
        }
        return new StockScreenerQueryBO.Query(
                parseDate(value.tradeDate()).orElse(null),
                normalizeUniverse(value.universe()),
                value.excludeSt() == null || value.excludeSt(),
                value.excludeChiNext() == null || value.excludeChiNext(),
                value.excludeStar() == null || value.excludeStar(),
                minPrice,
                maxPrice,
                value.minChangePct(),
                value.maxChangePct(),
                minAmount,
                minMarketCap,
                maxMarketCap,
                blankToNull(value.industry()),
                blankToNull(value.tdxIndustryCode()),
                blankToNull(value.thsIndustryCode()),
                blankToNull(value.conceptSectorId()),
                blankToNull(value.styleSectorId()),
                normalizeBoard(value.board()),
                value.minAmplitudePct(),
                value.maxAmplitudePct(),
                value.minReturnPct5d(),
                value.maxReturnPct5d(),
                value.minReturnPct10d(),
                value.maxReturnPct10d(),
                value.minReturnPct30d(),
                value.maxReturnPct30d(),
                value.minReturnPct60d(),
                value.maxReturnPct60d(),
                value.minReturnPct252d(),
                value.maxReturnPct252d(),
                value.minMainNet(),
                value.minMainNet5d(),
                value.minTurnoverPct(),
                value.maxTurnoverPct(),
                value.minVolumeRatio(),
                value.maxVolumeRatio(),
                value.minPe(),
                value.maxPe(),
                normalizeSort(value.sort()),
                normalizeSortDirection(value.sortDir()),
                new StockScreenerQueryBO.Page(pageNumber, pageSize, (pageNumber - 1) * pageSize),
                stockCode,
                stockMarket,
                normalizePriceVolumePattern(value.priceVolumePattern())
        );
    }

    public boolean matches(StockScreenerQueryBO.Row row, StockScreenerQueryBO.Query query) {
        if (row == null || row.price() == null) {
            return false;
        }
        if (!"ALL".equals(query.universe()) && !query.universe().equalsIgnoreCase(row.market())) {
            return false;
        }
        if (query.stockCode() != null && !query.stockCode().equals(row.code())) {
            return false;
        }
        if (query.stockMarket() != null && !query.stockMarket().equalsIgnoreCase(row.market())) {
            return false;
        }
        if (query.excludeSt() && row.st()) {
            return false;
        }
        if (query.excludeChiNext() && "chinext".equalsIgnoreCase(row.board())) return false;
        if (query.excludeStar() && "star".equalsIgnoreCase(row.board())) return false;
        if (query.industry() != null && !query.industry().equalsIgnoreCase(nullToEmpty(row.industry()))) {
            return false;
        }
        if (query.tdxIndustryCode() != null && !query.tdxIndustryCode().equalsIgnoreCase(nullToEmpty(row.tdxIndustryCode()))) {
            return false;
        }
        if (query.thsIndustryCode() != null && !query.thsIndustryCode().equalsIgnoreCase(nullToEmpty(row.thsIndustryCode()))) {
            return false;
        }
        if (query.conceptSectorId() != null && !containsSector(row.concepts(), query.conceptSectorId())) {
            return false;
        }
        if (query.styleSectorId() != null && !containsSector(row.styles(), query.styleSectorId())) {
            return false;
        }
        if (!"ALL".equals(query.board()) && !query.board().equalsIgnoreCase(nullToEmpty(row.board()))) {
            return false;
        }
        return inRange(row.price(), query.minPrice(), query.maxPrice())
                && inRange(row.changePct(), query.minChangePct(), query.maxChangePct())
                && atLeast(row.amount(), query.minAmount())
                && inRange(row.totalMarketCap(), query.minMarketCap(), query.maxMarketCap())
                && inRange(row.amplitudePct(), query.minAmplitudePct(), query.maxAmplitudePct())
                && inRange(row.returnPct5d(), query.minReturnPct5d(), query.maxReturnPct5d())
                && inRange(row.returnPct10d(), query.minReturnPct10d(), query.maxReturnPct10d())
                && inRange(row.returnPct30d(), query.minReturnPct30d(), query.maxReturnPct30d())
                && inRange(row.returnPct60d(), query.minReturnPct60d(), query.maxReturnPct60d())
                && inRange(row.returnPct252d(), query.minReturnPct252d(), query.maxReturnPct252d())
                && atLeast(row.mainNet(), query.minMainNet())
                && atLeast(row.mainNet5d(), query.minMainNet5d())
                && inRange(row.turnoverPct(), query.minTurnoverPct(), query.maxTurnoverPct())
                && inRange(row.volumeRatio(), query.minVolumeRatio(), query.maxVolumeRatio())
                && inRange(row.pe(), query.minPe(), query.maxPe())
                && matchesPriceVolumePattern(row.priceVolumePattern(), query.priceVolumePattern());
    }

    private StockScreenerQueryBO.PriceVolumePattern normalizePriceVolumePattern(
            StockScreenerQueryBO.PriceVolumePattern value) {
        if (value == null || !value.enabled()) return null;
        Integer contractionDays = value.volumeContractionDays();
        if (contractionDays != null && (contractionDays < 2 || contractionDays > 10)) {
            throw new IllegalArgumentException("volumeContractionDays must be between 2 and 10");
        }
        BigDecimal volumeMultiple = nonNegative(value.minBreakoutVolumeMultiple(), "minBreakoutVolumeMultiple");
        BigDecimal maxVolumeMultiple = nonNegative(value.maxBreakoutVolumeMultiple(), "maxBreakoutVolumeMultiple");
        if ((volumeMultiple != null || maxVolumeMultiple != null) && contractionDays == null) {
            throw new IllegalArgumentException("volumeContractionDays is required when breakout-volume range is set");
        }
        if (volumeMultiple != null && volumeMultiple.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("minBreakoutVolumeMultiple must be at least 1");
        }
        if (volumeMultiple != null && volumeMultiple.compareTo(new BigDecimal("20")) > 0) {
            throw new IllegalArgumentException("minBreakoutVolumeMultiple must not exceed 20");
        }
        if (maxVolumeMultiple != null && maxVolumeMultiple.compareTo(new BigDecimal("20")) > 0) {
            throw new IllegalArgumentException("maxBreakoutVolumeMultiple must not exceed 20");
        }
        ensureRange(volumeMultiple, maxVolumeMultiple, "breakoutVolumeMultiple");
        BigDecimal bullishBody = nonNegative(value.minBullishBodyPct(), "minBullishBodyPct");
        if (bullishBody != null && bullishBody.compareTo(new BigDecimal("30")) > 0) {
            throw new IllegalArgumentException("minBullishBodyPct must not exceed 30");
        }
        BigDecimal closeAboveMa20 = bounded(value.minCloseAboveMa20Pct(), "minCloseAboveMa20Pct", -100, 500);
        BigDecimal maSpread = bounded(value.minMa5AboveMa20Pct(), "minMa5AboveMa20Pct", -100, 300);
        Integer breakoutDays = value.breakoutHighDays();
        if (breakoutDays != null && (breakoutDays < 5 || breakoutDays > 20)) {
            throw new IllegalArgumentException("breakoutHighDays must be between 5 and 20");
        }
        BigDecimal atrPct20 = bounded(value.maxAtrPct20(), "maxAtrPct20", 0.01, 50);
        Integer bullishDays = value.minConsecutiveBullishDays();
        if (bullishDays != null && (bullishDays < 1 || bullishDays > 10)) {
            throw new IllegalArgumentException("minConsecutiveBullishDays must be between 1 and 10");
        }
        BigDecimal volumeVsMa5 = bounded(value.minVolumeVsMa5Multiple(), "minVolumeVsMa5Multiple", 0.1, 20);
        BigDecimal maxVolumeVsMa5 = bounded(value.maxVolumeVsMa5Multiple(), "maxVolumeVsMa5Multiple", 0.1, 20);
        ensureRange(volumeVsMa5, maxVolumeVsMa5, "volumeVsMa5Multiple");
        String candlestickPattern = normalizeCandlestickPattern(value.candlestickPattern());
        return new StockScreenerQueryBO.PriceVolumePattern(contractionDays, volumeMultiple, maxVolumeMultiple, bullishBody,
                closeAboveMa20, maSpread, breakoutDays, atrPct20, bullishDays, volumeVsMa5, maxVolumeVsMa5, candlestickPattern);
    }

    private boolean matchesPriceVolumePattern(StockScreenerQueryBO.PriceVolumePatternMetrics metrics,
                                               StockScreenerQueryBO.PriceVolumePattern pattern) {
        if (pattern == null || !pattern.enabled()) return true;
        if (metrics == null) return false;
        return (pattern.volumeContractionDays() == null
                || metrics.observedContractionDays() != null
                && metrics.observedContractionDays() >= pattern.volumeContractionDays())
                && (pattern.minBreakoutVolumeMultiple() == null
                || metrics.breakoutVolumeMultiple() != null
                && metrics.breakoutVolumeMultiple().compareTo(pattern.minBreakoutVolumeMultiple()) >= 0)
                && (pattern.maxBreakoutVolumeMultiple() == null
                || metrics.breakoutVolumeMultiple() != null
                && metrics.breakoutVolumeMultiple().compareTo(pattern.maxBreakoutVolumeMultiple()) <= 0)
                && (pattern.minBullishBodyPct() == null
                || metrics.bullishBodyPct() != null
                && metrics.bullishBodyPct().compareTo(pattern.minBullishBodyPct()) >= 0)
                && (pattern.minCloseAboveMa20Pct() == null
                || atLeast(metrics.closeAboveMa20Pct(), pattern.minCloseAboveMa20Pct()))
                && (pattern.minMa5AboveMa20Pct() == null
                || atLeast(metrics.ma5AboveMa20Pct(), pattern.minMa5AboveMa20Pct()))
                && (pattern.breakoutHighDays() == null
                || metrics.breakoutHighDays() != null && metrics.breakoutHighDays() >= pattern.breakoutHighDays())
                && (pattern.maxAtrPct20() == null
                || metrics.atrPct20() != null && metrics.atrPct20().compareTo(pattern.maxAtrPct20()) <= 0)
                && (pattern.minConsecutiveBullishDays() == null
                || metrics.consecutiveBullishDays() != null && metrics.consecutiveBullishDays() >= pattern.minConsecutiveBullishDays())
                && (pattern.minVolumeVsMa5Multiple() == null
                || atLeast(metrics.volumeVsMa5Multiple(), pattern.minVolumeVsMa5Multiple()))
                && (pattern.maxVolumeVsMa5Multiple() == null
                || metrics.volumeVsMa5Multiple() != null
                && metrics.volumeVsMa5Multiple().compareTo(pattern.maxVolumeVsMa5Multiple()) <= 0)
                && (pattern.candlestickPattern() == null
                || metrics.candlestickPatterns() != null && metrics.candlestickPatterns().contains(pattern.candlestickPattern()));

    }

    public int dailyQuoteHistorySessions(StockScreenerQueryBO.Query query) {
        int sessions = 31;
        if (query.minReturnPct60d() != null || query.maxReturnPct60d() != null || "return_60d".equals(query.sort())) sessions = 61;
        if (query.minReturnPct252d() != null || query.maxReturnPct252d() != null || "return_252d".equals(query.sort())) sessions = 253;
        return sessions;
    }

    private String normalizeCandlestickPattern(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!CANDLESTICK_PATTERNS.contains(normalized)) throw new IllegalArgumentException("candlestickPattern is unsupported");
        return normalized;
    }

    public Comparator<StockScreenerQueryBO.Row> comparator(StockScreenerQueryBO.Query query) {
        boolean ascending = "asc".equals(query.sortDir());
        Comparator<StockScreenerQueryBO.Row> comparator = switch (query.sort()) {
            case "price" -> nullable(StockScreenerQueryBO.Row::price, ascending);
            case "change_pct" -> nullable(StockScreenerQueryBO.Row::changePct, ascending);
            case "return_5d" -> nullable(StockScreenerQueryBO.Row::returnPct5d, ascending);
            case "return_10d" -> nullable(StockScreenerQueryBO.Row::returnPct10d, ascending);
            case "return_30d" -> nullable(StockScreenerQueryBO.Row::returnPct30d, ascending);
            case "return_60d" -> nullable(StockScreenerQueryBO.Row::returnPct60d, ascending);
            case "return_252d" -> nullable(StockScreenerQueryBO.Row::returnPct252d, ascending);
            case "main_net" -> nullable(StockScreenerQueryBO.Row::mainNet, ascending);
            case "main_net_5d" -> nullable(StockScreenerQueryBO.Row::mainNet5d, ascending);
            case "turnover_pct" -> nullable(StockScreenerQueryBO.Row::turnoverPct, ascending);
            case "volume_ratio" -> nullable(StockScreenerQueryBO.Row::volumeRatio, ascending);
            case "pe" -> nullable(StockScreenerQueryBO.Row::pe, ascending);
            case "amplitude_pct" -> nullable(StockScreenerQueryBO.Row::amplitudePct, ascending);
            case "market_cap" -> nullable(StockScreenerQueryBO.Row::totalMarketCap, ascending);
            case "code" -> nullableCode(ascending);
            default -> nullable(StockScreenerQueryBO.Row::amount, ascending);
        };
        return comparator.thenComparing(StockScreenerQueryBO.Row::code, Comparator.nullsLast(String::compareTo));
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim()));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("tradeDate must be yyyy-MM-dd", exception);
        }
    }

    private String normalizeUniverse(String value) {
        String normalized = value == null ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SH", "SZ", "BJ", "ALL" -> normalized;
            default -> "ALL";
        };
    }

    private String normalizeBoard(String value) {
        String normalized = value == null ? "ALL" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "main_sh", "main_sz", "chinext", "star", "bse", "unknown" -> normalized;
            default -> "ALL";
        };
    }

    private String normalizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "amount";
        }
        return switch (value.trim()) {
            case "price", "change_pct", "amplitude_pct", "return_5d", "return_10d", "return_30d", "return_60d", "return_252d", "main_net", "main_net_5d", "turnover_pct", "volume_ratio", "pe", "amount", "market_cap", "code" -> value.trim();
            default -> "amount";
        };
    }

    private String normalizeSortDirection(String value) {
        return value != null && "asc".equalsIgnoreCase(value.trim()) ? "asc" : "desc";
    }

    private String normalizeStockCode(String value) {
        String normalized = blankToNull(value);
        if (normalized != null && !normalized.matches("\\d{6}")) {
            throw new IllegalArgumentException("stockCode must be a six-digit A-share code");
        }
        return normalized;
    }

    private String normalizeStockMarket(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "SH", "SZ", "BJ" -> normalized.toUpperCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("stockMarket must be SH, SZ, or BJ");
        };
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private BigDecimal bounded(BigDecimal value, String field, double min, double max) {
        if (value == null) return null;
        if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    private void ensureRange(BigDecimal min, BigDecimal max, String field) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min " + field + " must not exceed max " + field);
        }
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return min == null && max == null;
        }
        return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
    }

    private boolean atLeast(BigDecimal value, BigDecimal minimum) {
        return minimum == null || (value != null && value.compareTo(minimum) >= 0);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean containsSector(java.util.List<StockScreenerQueryBO.SectorMembership> memberships, String sectorId) {
        return memberships != null && memberships.stream().anyMatch(item -> sectorId.equalsIgnoreCase(item.sectorId()));
    }

    private Comparator<StockScreenerQueryBO.Row> nullable(java.util.function.Function<StockScreenerQueryBO.Row, BigDecimal> getter,
                                                           boolean ascending) {
        Comparator<BigDecimal> values = ascending ? BigDecimal::compareTo : Comparator.reverseOrder();
        return Comparator.comparing(getter, Comparator.nullsLast(values));
    }

    private Comparator<StockScreenerQueryBO.Row> nullableCode(boolean ascending) {
        Comparator<String> values = ascending ? String::compareTo : Comparator.reverseOrder();
        return Comparator.comparing(StockScreenerQueryBO.Row::code, Comparator.nullsLast(values));
    }
}
