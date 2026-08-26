package cn.cccs7.cynexus.market.domain.entity.screener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StockScreenerQueryBO {

    private StockScreenerQueryBO() {
    }

    public record QueryInput(String tradeDate,
                              String universe,
                              Boolean excludeSt,
                              Boolean excludeChiNext,
                              Boolean excludeStar,
                             BigDecimal minPrice,
                             BigDecimal maxPrice,
                             BigDecimal minChangePct,
                             BigDecimal maxChangePct,
                             BigDecimal minAmount,
                             BigDecimal minMarketCap,
                             BigDecimal maxMarketCap,
                             String industry,
                             String tdxIndustryCode,
                             String thsIndustryCode,
                             String conceptSectorId,
                             String styleSectorId,
                             String board,
                             BigDecimal minAmplitudePct,
                             BigDecimal maxAmplitudePct,
                             BigDecimal minReturnPct5d,
                             BigDecimal maxReturnPct5d,
                             BigDecimal minReturnPct10d,
                             BigDecimal maxReturnPct10d,
                             BigDecimal minReturnPct30d,
                             BigDecimal maxReturnPct30d,
                             BigDecimal minReturnPct60d,
                             BigDecimal maxReturnPct60d,
                             BigDecimal minReturnPct252d,
                             BigDecimal maxReturnPct252d,
                             BigDecimal minMainNet,
                             BigDecimal minMainNet5d,
                             BigDecimal minTurnoverPct,
                             BigDecimal maxTurnoverPct,
                             BigDecimal minVolumeRatio,
                             BigDecimal maxVolumeRatio,
                             BigDecimal minPe,
                             BigDecimal maxPe,
                             String sort,
                             String sortDir,
                             Integer page,
                             Integer pageSize,
                             String stockCode,
                             String stockMarket,
                             PriceVolumePattern priceVolumePattern) {
    }

    public record Query(LocalDate requestedTradeDate,
                        String universe,
                        boolean excludeSt,
                        boolean excludeChiNext,
                        boolean excludeStar,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        BigDecimal minChangePct,
                        BigDecimal maxChangePct,
                        BigDecimal minAmount,
                        BigDecimal minMarketCap,
                        BigDecimal maxMarketCap,
                        String industry,
                        String tdxIndustryCode,
                        String thsIndustryCode,
                        String conceptSectorId,
                        String styleSectorId,
                        String board,
                        BigDecimal minAmplitudePct,
                        BigDecimal maxAmplitudePct,
                        BigDecimal minReturnPct5d,
                        BigDecimal maxReturnPct5d,
                        BigDecimal minReturnPct10d,
                        BigDecimal maxReturnPct10d,
                        BigDecimal minReturnPct30d,
                        BigDecimal maxReturnPct30d,
                        BigDecimal minReturnPct60d,
                        BigDecimal maxReturnPct60d,
                        BigDecimal minReturnPct252d,
                        BigDecimal maxReturnPct252d,
                        BigDecimal minMainNet,
                        BigDecimal minMainNet5d,
                        BigDecimal minTurnoverPct,
                        BigDecimal maxTurnoverPct,
                        BigDecimal minVolumeRatio,
                        BigDecimal maxVolumeRatio,
                        BigDecimal minPe,
                        BigDecimal maxPe,
                        String sort,
                        String sortDir,
                        Page page,
                        String stockCode,
                        String stockMarket,
                        PriceVolumePattern priceVolumePattern) {
    }

    public record Page(int number, int size, int offset) {
    }

    public record Snapshot(LocalDate tradeDate, List<Row> rows, String source, int eligibleStockCount) {
    }

    public record Row(String code,
                      String name,
                      String market,
                      String industry,
                      String board,
                      boolean st,
                      BigDecimal price,
                      BigDecimal changePct,
                      BigDecimal amplitudePct,
                      BigDecimal amount,
                      BigDecimal totalMarketCap,
                      BigDecimal floatMarketCap,
                      BigDecimal returnPct5d,
                      BigDecimal returnPct10d,
                      BigDecimal returnPct30d,
                      BigDecimal returnPct60d,
                      BigDecimal returnPct252d,
                      BigDecimal mainNet,
                      BigDecimal mainNet5d,
                      String tdxIndustryCode,
                      String tdxIndustryName,
                      String thsIndustryCode,
                      String thsIndustryName,
                      List<SectorMembership> concepts,
                      List<SectorMembership> styles,
                      BigDecimal turnoverPct,
                      BigDecimal volumeRatio,
                      BigDecimal pe,
                      PriceVolumePatternMetrics priceVolumePattern) {
    }

    public record Result(boolean ok,
                         LocalDate requestedTradeDate,
                         LocalDate tradeDate,
                         String universe,
                         boolean excludeSt,
                         boolean excludeChiNext,
                         boolean excludeStar,
                         String industry,
                         String tdxIndustryCode,
                         String thsIndustryCode,
                         String conceptSectorId,
                         String styleSectorId,
                         String board,
                         String sort,
                         String sortDir,
                         Page page,
                         int universeCount,
                         int count,
                         String source,
                         List<Row> items,
                         List<String> warnings,
                         String error,
                         DataQuality dataQuality) {
    }

    public record DataQuality(String status,
                              LocalDate snapshotDate,
                              long freshnessLagDays,
                              int eligibleStockCount,
                              int dailySnapshotCount,
                              List<FieldCoverage> fieldCoverage,
                              List<String> warnings) {}

    public record FieldCoverage(String field, int availableCount, int eligibleCount, BigDecimal coveragePct) {}

    public record ClassificationOption(String code, String name, int stockCount, LocalDate snapshotDate, String source) {}

    public record ClassificationOptions(List<ClassificationOption> tdxIndustryGroups,
                                        List<ClassificationOption> thsIndustries,
                                        List<ClassificationOption> concepts,
                                        List<ClassificationOption> styles) {}

    public record SectorMembership(String sectorId, String name) {}

    /** A bounded daily price-volume shape derived from the durable OHLCV projection. */
    public record PriceVolumePattern(Integer volumeContractionDays,
                                     BigDecimal minBreakoutVolumeMultiple,
                                     BigDecimal maxBreakoutVolumeMultiple,
                                     BigDecimal minBullishBodyPct,
                                     BigDecimal minCloseAboveMa20Pct,
                                     BigDecimal minMa5AboveMa20Pct,
                                     Integer breakoutHighDays,
                                     BigDecimal maxAtrPct20,
                                     Integer minConsecutiveBullishDays,
                                     BigDecimal minVolumeVsMa5Multiple,
                                     BigDecimal maxVolumeVsMa5Multiple,
                                     String candlestickPattern) {
        public PriceVolumePattern(Integer volumeContractionDays,
                                  BigDecimal minBreakoutVolumeMultiple,
                                  BigDecimal minBullishBodyPct) {
            this(volumeContractionDays, minBreakoutVolumeMultiple, null, minBullishBodyPct,
                    null, null, null, null, null, null, null, null);
        }

        public boolean enabled() {
            return volumeContractionDays != null || minBreakoutVolumeMultiple != null || minBullishBodyPct != null
                    || minCloseAboveMa20Pct != null || minMa5AboveMa20Pct != null || breakoutHighDays != null
                    || maxAtrPct20 != null || minConsecutiveBullishDays != null || minVolumeVsMa5Multiple != null
                    || maxBreakoutVolumeMultiple != null || maxVolumeVsMa5Multiple != null || candlestickPattern != null;
        }
    }

    /** Bounded daily-close technical measurements sourced from the durable OHLCV fact. */
    public record PriceVolumePatternMetrics(Integer observedContractionDays,
                                            BigDecimal breakoutVolumeMultiple,
                                            BigDecimal bullishBodyPct,
                                            BigDecimal closeAboveMa20Pct,
                                             BigDecimal ma5AboveMa20Pct,
                                            Integer breakoutHighDays,
                                             BigDecimal atrPct20,
                                             Integer consecutiveBullishDays,
                                             BigDecimal volumeVsMa5Multiple,
                                             List<String> candlestickPatterns) {
        public PriceVolumePatternMetrics(Integer observedContractionDays,
                                         BigDecimal breakoutVolumeMultiple,
                                         BigDecimal bullishBodyPct) {
            this(observedContractionDays, breakoutVolumeMultiple, bullishBodyPct,
                    null, null, null, null, null, null, List.of());
        }
    }
}
