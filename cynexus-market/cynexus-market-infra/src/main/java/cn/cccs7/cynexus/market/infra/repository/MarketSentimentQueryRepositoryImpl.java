package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndexDailyBarBO;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketSentimentLimitEmotionDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import cn.cccs7.cynexus.market.domain.repository.MarketSentimentQueryRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketSentimentQueryBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.convert.MaCountDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketOverviewPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketSentimentLimitEmotionDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MsiIndexDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.ProfitEffectDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.RiskAppetiteDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.SectorBreadthDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.StyleRiskDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.TurnoverActivityDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.VolatilityDailyPOConverter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketSentimentQueryRepositoryImpl implements MarketSentimentQueryRepository {

    private final MarketSentimentQueryBasicService basicService;
    private final ClickHouseStockChartClient clickHouseStockChartClient;

    public MarketSentimentQueryRepositoryImpl(MarketSentimentQueryBasicService basicService,
                                              ClickHouseStockChartClient clickHouseStockChartClient) {
        this.basicService = basicService;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
    }

    @Override
    public Optional<MaCountDailyBO> findMaCount(LocalDate tradeDate) {
        return basicService.findMaCount(tradeDate).map(MaCountDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MaCountDailyBO> listMaCount(LocalDate startDate, LocalDate endDate) {
        return MaCountDailyPOConverter.INSTANCE.toTargetList(basicService.listMaCount(startDate, endDate));
    }

    @Override
    public Optional<RiskAppetiteDailyBO> findRiskAppetite(LocalDate tradeDate) {
        return basicService.findRiskAppetite(tradeDate).map(RiskAppetiteDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<RiskAppetiteDailyBO> listRiskAppetite(LocalDate startDate, LocalDate endDate) {
        return RiskAppetiteDailyPOConverter.INSTANCE.toTargetList(basicService.listRiskAppetite(startDate, endDate));
    }

    @Override
    public Optional<MarketSentimentLimitEmotionDailyBO> findLimitEmotion(LocalDate tradeDate) {
        return basicService.findLimitEmotion(tradeDate).map(MarketSentimentLimitEmotionDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarketSentimentLimitEmotionDailyBO> listLimitEmotion(LocalDate startDate, LocalDate endDate) {
        return MarketSentimentLimitEmotionDailyPOConverter.INSTANCE.toTargetList(basicService.listLimitEmotion(startDate, endDate));
    }

    @Override
    public Optional<VolatilityDailyBO> findVolatility(LocalDate tradeDate) {
        return basicService.findVolatility(tradeDate).map(VolatilityDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<VolatilityDailyBO> listVolatility(LocalDate startDate, LocalDate endDate) {
        return VolatilityDailyPOConverter.INSTANCE.toTargetList(basicService.listVolatility(startDate, endDate));
    }

    @Override
    public Optional<TurnoverActivityDailyBO> findTurnover(LocalDate tradeDate) {
        return basicService.findTurnover(tradeDate).map(TurnoverActivityDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<TurnoverActivityDailyBO> listTurnover(LocalDate startDate, LocalDate endDate) {
        return TurnoverActivityDailyPOConverter.INSTANCE.toTargetList(basicService.listTurnover(startDate, endDate));
    }

    @Override
    public Optional<StyleRiskDailyBO> findStyleRisk(LocalDate tradeDate) {
        return basicService.findStyleRisk(tradeDate).map(StyleRiskDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<StyleRiskDailyBO> listStyleRisk(LocalDate startDate, LocalDate endDate) {
        return StyleRiskDailyPOConverter.INSTANCE.toTargetList(basicService.listStyleRisk(startDate, endDate));
    }

    @Override
    public Optional<ProfitEffectDailyBO> findProfitEffect(LocalDate tradeDate) {
        return basicService.findProfitEffect(tradeDate).map(ProfitEffectDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<ProfitEffectDailyBO> listProfitEffect(LocalDate startDate, LocalDate endDate) {
        return ProfitEffectDailyPOConverter.INSTANCE.toTargetList(basicService.listProfitEffect(startDate, endDate));
    }

    @Override
    public Optional<SectorBreadthDailyBO> findSectorBreadth(LocalDate tradeDate) {
        return basicService.findSectorBreadth(tradeDate).map(SectorBreadthDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<SectorBreadthDailyBO> listSectorBreadth(LocalDate startDate, LocalDate endDate) {
        return SectorBreadthDailyPOConverter.INSTANCE.toTargetList(basicService.listSectorBreadth(startDate, endDate));
    }

    @Override
    public Optional<MsiIndexDailyBO> findMsiIndex(LocalDate tradeDate) {
        return basicService.findMsiIndex(tradeDate).map(MsiIndexDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MsiIndexDailyBO> listMsiIndex(LocalDate startDate, LocalDate endDate) {
        return MsiIndexDailyPOConverter.INSTANCE.toTargetList(basicService.listMsiIndex(startDate, endDate));
    }

    @Override
    public List<MarketOverviewBO> listOverview(LocalDate startDate, LocalDate endDate) {
        return MarketOverviewPOConverter.INSTANCE.toTargetList(basicService.listOverview(startDate, endDate));
    }

    @Override
    public List<IndexDailyBarBO> listIndexDaily(String code, LocalDate startDate, LocalDate endDate) {
        ClickHouseStockChartClient.MarketSymbol normalizedSymbol = normalizeIndexSymbol(code);
        return clickHouseStockChartClient.listDailyBars(normalizedSymbol.market(), normalizedSymbol.symbol(), startDate, endDate).stream()
                .filter(bar -> bar.getTradeDate() != null)
                .map(bar -> IndexDailyBarBO.builder()
                        .tradeDate(bar.getTradeDate())
                        .code(toIndexCode(normalizedSymbol))
                        .name(indexName(code))
                        .close(bar.getClose())
                        .amount(bar.getAmount())
                        .build())
                .toList();
    }

    private ClickHouseStockChartClient.MarketSymbol normalizeIndexSymbol(String code) {
        if (code == null) {
            return new ClickHouseStockChartClient.MarketSymbol("SH", "");
        }
        String value = code.trim().toLowerCase();
        if (value.startsWith("sh") || value.startsWith("sz")) {
            return new ClickHouseStockChartClient.MarketSymbol(value.substring(0, 2).toUpperCase(), value.substring(2));
        }
        String digits = value.replaceAll("\\D", "");
        return switch (digits) {
            case "399001", "399006" -> new ClickHouseStockChartClient.MarketSymbol("SZ", digits);
            default -> new ClickHouseStockChartClient.MarketSymbol("SH", digits);
        };
    }

    private String toIndexCode(ClickHouseStockChartClient.MarketSymbol symbol) {
        if (symbol == null || symbol.symbol() == null) {
            return "";
        }
        return symbol.market().toLowerCase() + symbol.symbol();
    }

    private String indexName(String code) {
        String digits = code == null ? "" : code.replaceAll("\\D", "");
        return switch (digits) {
            case "000001" -> "上证指数";
            case "399001" -> "深证成指";
            case "399006" -> "创业板指";
            default -> digits.isBlank() ? code : digits;
        };
    }
}
