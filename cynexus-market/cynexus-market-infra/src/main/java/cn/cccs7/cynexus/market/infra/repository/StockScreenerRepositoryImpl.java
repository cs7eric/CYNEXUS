package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.repository.StockScreenerRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockBasicMapper;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockScreenerFundFlowClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockScreenerIndustryClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockScreenerSectorClient;
import cn.cccs7.cynexus.market.infra.client.PostgresStockScreenerPriceVolumePatternClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class StockScreenerRepositoryImpl implements StockScreenerRepository {

    private static final String SOURCE = "clickhouse.daily_raw + postgres.sec_stock_basic + postgres.a_stock_fund_flow_daily + postgres.ths_industry_constituent_snapshot + postgres.sec_stock_sector";

    private final StockBasicMapper stockBasicMapper;
    private final ClickHouseStockChartClient clickHouseStockChartClient;
    private final PostgresStockScreenerFundFlowClient fundFlowClient;
    private final PostgresStockScreenerIndustryClient industryClient;
    private final PostgresStockScreenerSectorClient sectorClient;
    private final PostgresStockScreenerPriceVolumePatternClient priceVolumePatternClient;

    public StockScreenerRepositoryImpl(StockBasicMapper stockBasicMapper,
                                       ClickHouseStockChartClient clickHouseStockChartClient,
                                       PostgresStockScreenerFundFlowClient fundFlowClient,
                                       PostgresStockScreenerIndustryClient industryClient,
                                       PostgresStockScreenerSectorClient sectorClient,
                                       PostgresStockScreenerPriceVolumePatternClient priceVolumePatternClient) {
        this.stockBasicMapper = stockBasicMapper;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
        this.fundFlowClient = fundFlowClient;
        this.industryClient = industryClient;
        this.sectorClient = sectorClient;
        this.priceVolumePatternClient = priceVolumePatternClient;
    }

    @Override
    public Optional<StockScreenerQueryBO.Snapshot> findSnapshotOnOrBefore(LocalDate requestedTradeDate,
                                                                             StockScreenerQueryBO.PriceVolumePattern priceVolumePattern,
                                                                             int dailyHistorySessions) {
        List<StockBasicPO> basics = stockBasicMapper.selectList(new LambdaQueryWrapper<StockBasicPO>()
                        .select(StockBasicPO::getCode, StockBasicPO::getName, StockBasicPO::getMarket,
                                StockBasicPO::getIndustry, StockBasicPO::getBoard, StockBasicPO::getSt, StockBasicPO::getTotalMarketCap,
                                StockBasicPO::getFloatMarketCap)
                        .eq(StockBasicPO::getActive, true))
                .stream()
                .filter(item -> item.getCode() != null && item.getCode().matches("\\d{6}"))
                .filter(item -> market(item.getMarket()) != null)
                .toList();
        if (basics.isEmpty()) {
            return Optional.empty();
        }
        List<ClickHouseStockChartClient.DailyQuote> quotes = clickHouseStockChartClient
                .listLatestAsharesDailyQuotesOnOrBefore(requestedTradeDate, dailyHistorySessions);
        LocalDate tradeDate = quotes.stream().map(ClickHouseStockChartClient.DailyQuote::getTradeDate)
                .filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null);
        if (tradeDate == null) {
            return Optional.empty();
        }
        Map<String, PostgresStockScreenerFundFlowClient.FundFlowSummary> fundFlowByCode = fundFlowClient.findOnTradeDate(tradeDate);
        Map<String, PostgresStockScreenerIndustryClient.IndustryMetrics> industryByCode = industryClient.findOnTradeDate(tradeDate);
        Map<String, PostgresStockScreenerSectorClient.Memberships> sectorMembershipByCode = sectorClient.findMemberships();
        Map<String, StockScreenerQueryBO.PriceVolumePatternMetrics> priceVolumePatterns =
                priceVolumePatternClient.findOnTradeDate(tradeDate, priceVolumePattern);
        Map<String, ClickHouseStockChartClient.DailyQuote> dailyByMarketAndCode = quotes.stream()
                .filter(item -> tradeDate.equals(item.getTradeDate()))
                .collect(Collectors.toMap(item -> quoteKey(item.getMarket(), item.getSymbol()), Function.identity(), (left, right) -> left));
        List<StockScreenerQueryBO.Row> rows = basics.stream()
                .map(item -> row(item, dailyByMarketAndCode.get(quoteKey(market(item.getMarket()), item.getCode())),
                        fundFlowByCode.get(item.getCode()), industryByCode.get(item.getCode()), sectorMembershipByCode.get(item.getCode()),
                        priceVolumePatterns.get(item.getCode())))
                .filter(item -> item != null)
                .toList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(new StockScreenerQueryBO.Snapshot(tradeDate, rows, SOURCE, basics.size()));
    }

    @Override
    public StockScreenerQueryBO.ClassificationOptions findClassificationOptionsOnOrBefore(LocalDate requestedTradeDate) {
        return new StockScreenerQueryBO.ClassificationOptions(
                industryClient.listTdxGroupOptionsOnOrBefore(requestedTradeDate).stream()
                        .map(item -> option(item.code(), item.name(), item.stockCount(), item.snapshotDate(), "postgres.ths_industry_constituent_snapshot + postgres.tdx_ths_industry_mapping")).toList(),
                industryClient.listOptionsOnOrBefore(requestedTradeDate).stream()
                        .map(item -> option(item.code(), item.name(), item.stockCount(), item.snapshotDate(), "postgres.ths_industry_constituent_snapshot")).toList(),
                sectorClient.listOptions("concepts").stream()
                        .map(item -> option(item.sectorId(), item.name(), item.stockCount(), null, "postgres.sec_sector + postgres.sec_stock_sector")).toList(),
                sectorClient.listOptions("styles").stream()
                        .map(item -> option(item.sectorId(), item.name(), item.stockCount(), null, "postgres.sec_sector + postgres.sec_stock_sector")).toList());
    }

    private StockScreenerQueryBO.Row row(StockBasicPO basic, ClickHouseStockChartClient.DailyQuote quote,
                                         PostgresStockScreenerFundFlowClient.FundFlowSummary fundFlow,
                                         PostgresStockScreenerIndustryClient.IndustryMetrics industry,
                                         PostgresStockScreenerSectorClient.Memberships memberships,
                                         StockScreenerQueryBO.PriceVolumePatternMetrics priceVolumePattern) {
        if (quote == null || quote.getClose() == null) {
            return null;
        }
        return new StockScreenerQueryBO.Row(
                basic.getCode(), basic.getName(), market(basic.getMarket()), basic.getIndustry(), basic.getBoard(), Boolean.TRUE.equals(basic.getSt()),
                quote.getClose(), quote.getChangePct(), quote.getAmplitude(), quote.getAmount(),
                basic.getTotalMarketCap(), basic.getFloatMarketCap(), quote.getReturnPct5d(), quote.getReturnPct10d(),
                quote.getReturnPct30d(), quote.getReturnPct60d(), quote.getReturnPct252d(),
                fundFlow == null ? null : fundFlow.mainNet(), fundFlow == null ? null : fundFlow.mainNet5d(),
                industry == null ? null : industry.tdxCode(), industry == null ? null : industry.tdxName(),
                industry == null ? null : industry.code(), industry == null ? null : industry.name(),
                memberships == null ? List.of() : memberships.concepts().stream()
                        .map(item -> new StockScreenerQueryBO.SectorMembership(item.sectorId(), item.name())).toList(),
                memberships == null ? List.of() : memberships.styles().stream()
                        .map(item -> new StockScreenerQueryBO.SectorMembership(item.sectorId(), item.name())).toList(),
                industry == null ? null : industry.turnoverPct(), industry == null ? null : industry.volumeRatio(),
                industry == null ? null : industry.pe(), priceVolumePattern
        );
    }

    private StockScreenerQueryBO.ClassificationOption option(String code, String name, int stockCount,
                                                              LocalDate snapshotDate, String source) {
        return new StockScreenerQueryBO.ClassificationOption(code, name, stockCount, snapshotDate, source);
    }

    private String quoteKey(String market, String code) {
        return market + ":" + code;
    }

    private String market(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("SH") || normalized.contains("沪")) {
            return "SH";
        }
        if (normalized.contains("SZ") || normalized.contains("深")) {
            return "SZ";
        }
        if (normalized.contains("BJ") || normalized.contains("北")) {
            return "BJ";
        }
        return null;
    }
}
