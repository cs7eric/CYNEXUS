package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockScreenerDTO;
import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockScreenerAppService {

    private final StockScreenerQueryDomainService domainService;

    public StockScreenerAppService(StockScreenerQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public StockScreenerDTO.Response screen(String tradeDate,
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
                                            Integer volumeContractionDays,
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
        StockScreenerQueryBO.Result result = domainService.screen(new StockScreenerQueryBO.QueryInput(
                tradeDate, universe, excludeSt, excludeChiNext, excludeStar, minPrice, maxPrice, minChangePct, maxChangePct,
                minAmount, minMarketCap, maxMarketCap, industry, tdxIndustryCode, thsIndustryCode, conceptSectorId, styleSectorId, board, minAmplitudePct, maxAmplitudePct,
                minReturnPct5d, maxReturnPct5d, minReturnPct10d, maxReturnPct10d, minReturnPct30d,
                maxReturnPct30d, minReturnPct60d, maxReturnPct60d, minReturnPct252d, maxReturnPct252d,
                minMainNet, minMainNet5d, minTurnoverPct, maxTurnoverPct, minVolumeRatio, maxVolumeRatio,
                minPe, maxPe, sort, sortDir, page, pageSize, null, null,
                new StockScreenerQueryBO.PriceVolumePattern(volumeContractionDays, minBreakoutVolumeMultiple, maxBreakoutVolumeMultiple, minBullishBodyPct,
                        minCloseAboveMa20Pct, minMa5AboveMa20Pct, breakoutHighDays, maxAtrPct20,
                        minConsecutiveBullishDays, minVolumeVsMa5Multiple, maxVolumeVsMa5Multiple, candlestickPattern)
        ));
        return StockScreenerDTO.Response.builder()
                .ok(result.ok())
                .requestedTradeDate(date(result.requestedTradeDate()))
                .tradeDate(date(result.tradeDate()))
                .universe(result.universe())
                .excludeSt(result.excludeSt())
                .excludeChiNext(result.excludeChiNext())
                .excludeStar(result.excludeStar())
                .industry(result.industry())
                .tdxIndustryCode(result.tdxIndustryCode())
                .thsIndustryCode(result.thsIndustryCode())
                .conceptSectorId(result.conceptSectorId())
                .styleSectorId(result.styleSectorId())
                .board(result.board())
                .sort(result.sort())
                .sortDir(result.sortDir())
                .page(result.page().number())
                .pageSize(result.page().size())
                .universeCount(result.universeCount())
                .count(result.count())
                .source(result.source())
                .items(result.items().stream().map(this::item).toList())
                .warnings(result.warnings())
                .error(result.error())
                .dataQuality(dataQuality(result.dataQuality()))
                .build();
    }

    private StockScreenerDTO.Item item(StockScreenerQueryBO.Row row) {
        return StockScreenerDTO.Item.builder()
                .code(row.code()).name(row.name()).market(row.market()).industry(row.industry()).board(row.board()).st(row.st())
                .price(row.price()).changePct(row.changePct()).amplitudePct(row.amplitudePct()).amount(row.amount())
                .totalMarketCap(row.totalMarketCap()).floatMarketCap(row.floatMarketCap())
                .returnPct5d(row.returnPct5d()).returnPct10d(row.returnPct10d()).returnPct30d(row.returnPct30d())
                .returnPct60d(row.returnPct60d()).returnPct252d(row.returnPct252d())
                .mainNet(row.mainNet()).mainNet5d(row.mainNet5d())
                .tdxIndustryCode(row.tdxIndustryCode()).tdxIndustryName(row.tdxIndustryName())
                .thsIndustryCode(row.thsIndustryCode()).thsIndustryName(row.thsIndustryName())
                .concepts(row.concepts().stream().map(item -> StockScreenerDTO.SectorMembership.builder()
                        .sectorId(item.sectorId()).name(item.name()).build()).toList())
                .styles(row.styles().stream().map(item -> StockScreenerDTO.SectorMembership.builder()
                        .sectorId(item.sectorId()).name(item.name()).build()).toList())
                .turnoverPct(row.turnoverPct()).volumeRatio(row.volumeRatio()).pe(row.pe())
                .priceVolumePattern(priceVolumePattern(row.priceVolumePattern()))
                .build();
    }

    private StockScreenerDTO.PriceVolumePatternMetrics priceVolumePattern(StockScreenerQueryBO.PriceVolumePatternMetrics value) {
        if (value == null) return null;
        return StockScreenerDTO.PriceVolumePatternMetrics.builder().observedContractionDays(value.observedContractionDays())
                .breakoutVolumeMultiple(value.breakoutVolumeMultiple()).bullishBodyPct(value.bullishBodyPct())
                .closeAboveMa20Pct(value.closeAboveMa20Pct()).ma5AboveMa20Pct(value.ma5AboveMa20Pct())
                .breakoutHighDays(value.breakoutHighDays()).atrPct20(value.atrPct20())
                .consecutiveBullishDays(value.consecutiveBullishDays()).volumeVsMa5Multiple(value.volumeVsMa5Multiple())
                .candlestickPatterns(value.candlestickPatterns()).build();
    }

    public List<StockScreenerDTO.IndustryOption> listIndustryOptions(String tradeDate) {
        return domainService.listClassificationOptions(tradeDate).thsIndustries().stream().map(this::option)
                .toList();
    }

    public StockScreenerDTO.ClassificationOptions listClassificationOptions(String tradeDate) {
        StockScreenerQueryBO.ClassificationOptions options = domainService.listClassificationOptions(tradeDate);
        return StockScreenerDTO.ClassificationOptions.builder()
                .tdxIndustryGroups(options.tdxIndustryGroups().stream().map(this::option).toList())
                .thsIndustries(options.thsIndustries().stream().map(this::option).toList())
                .concepts(options.concepts().stream().map(this::option).toList())
                .styles(options.styles().stream().map(this::option).toList())
                .build();
    }

    private StockScreenerDTO.IndustryOption option(StockScreenerQueryBO.ClassificationOption item) {
        return StockScreenerDTO.IndustryOption.builder().code(item.code()).name(item.name())
                .stockCount(item.stockCount()).snapshotDate(date(item.snapshotDate())).source(item.source()).build();
    }

    private StockScreenerDTO.DataQuality dataQuality(StockScreenerQueryBO.DataQuality value) {
        if (value == null) return null;
        return StockScreenerDTO.DataQuality.builder().status(value.status()).snapshotDate(date(value.snapshotDate()))
                .freshnessLagDays(value.freshnessLagDays()).eligibleStockCount(value.eligibleStockCount())
                .dailySnapshotCount(value.dailySnapshotCount()).warnings(value.warnings())
                .fieldCoverage(value.fieldCoverage().stream().map(item -> StockScreenerDTO.FieldCoverage.builder()
                        .field(item.field()).availableCount(item.availableCount()).eligibleCount(item.eligibleCount())
                        .coveragePct(item.coveragePct()).build()).toList()).build();
    }

    private String date(java.time.LocalDate value) {
        return value == null ? null : value.toString();
    }
}
