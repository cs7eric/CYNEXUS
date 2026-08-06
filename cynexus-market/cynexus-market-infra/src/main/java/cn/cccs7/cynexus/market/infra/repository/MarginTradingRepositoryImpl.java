package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustryHistoryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryConstituentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.service.MarginTradingBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.convert.MarginAccountStatsPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginIndustrySummaryPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginIndustryHistoryPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginMarketSummaryPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginStockDetailPOConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MarginTradingRepositoryImpl implements MarginTradingRepository {

    private final MarginTradingBasicService marginTradingBasicService;
    private final ClickHouseStockChartClient clickHouseStockChartClient;

    public MarginTradingRepositoryImpl(
            MarginTradingBasicService marginTradingBasicService,
            ClickHouseStockChartClient clickHouseStockChartClient
    ) {
        this.marginTradingBasicService = marginTradingBasicService;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
    }

    @Override
    public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
        return marginTradingBasicService.findLatestTradeDate(endDate);
    }

    @Override
    public List<MarginMarketSummaryBO> listMarketSummaryByTradeDate(LocalDate tradeDate) {
        return MarginMarketSummaryPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listMarketSummaryByTradeDate(tradeDate)
        );
    }

    @Override
    public Optional<MarginAccountStatsBO> findAccountStatsByTradeDate(LocalDate tradeDate) {
        return marginTradingBasicService.findAccountStatsByTradeDate(tradeDate)
                .map(MarginAccountStatsPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarginMarketSummaryBO> listMarketSummaryHistory(LocalDate startDate, LocalDate endDate) {
        return MarginMarketSummaryPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listMarketSummaryHistory(startDate, endDate)
        );
    }

    @Override
    public List<MarginStockDetailBO> listStocks(LocalDate tradeDate, String exchange, String industryCode, String sort, String sortDir, int offset, int limit) {
        return MarginStockDetailPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listStocks(tradeDate, exchange, industryCode, sort, sortDir, offset, limit)
        );
    }

    @Override
    public int countStocks(LocalDate tradeDate, String exchange, String industryCode) {
        return marginTradingBasicService.countStocks(tradeDate, exchange, industryCode);
    }

    @Override
    public List<MarginStockDetailBO> listStockHistory(String stockCode, int limit) {
        return MarginStockDetailPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listStockHistory(stockCode, limit)
        );
    }

    @Override
    public List<MarginIndustrySummaryBO> listIndustrySummaries(LocalDate tradeDate, String sort, String sortDir, int offset, int limit) {
        List<MarginIndustrySummaryPO> rows = marginTradingBasicService.listIndustrySummaries(
                tradeDate, sort, sortDir, offset, limit
        );
        enrichIndustrySummaryTurnover(rows, tradeDate);
        return MarginIndustrySummaryPOConverter.INSTANCE.toTargetList(rows);
    }

    @Override
    public int countIndustrySummaries(LocalDate tradeDate) {
        return marginTradingBasicService.countIndustrySummaries(tradeDate);
    }

    @Override
    public List<MarginIndustryHistoryBO> listIndustryHistory(LocalDate tradeDate, String industryCode, int limit) {
        List<MarginIndustryHistoryPO> rows = marginTradingBasicService.listIndustryHistory(tradeDate, industryCode, limit);
        if (!rows.isEmpty()) {
            LocalDate startDate = rows.stream()
                    .map(MarginIndustryHistoryPO::getTradeDate)
                    .min(LocalDate::compareTo)
                    .orElse(tradeDate);
            LocalDate endDate = rows.stream()
                    .map(MarginIndustryHistoryPO::getTradeDate)
                    .max(LocalDate::compareTo)
                    .orElse(tradeDate);
            enrichIndustryHistoryTurnover(rows, tradeDate, startDate, endDate);
        }
        return MarginIndustryHistoryPOConverter.INSTANCE.toTargetList(rows);
    }

    private void enrichIndustrySummaryTurnover(List<MarginIndustrySummaryPO> rows, LocalDate tradeDate) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> industryCodes = rows.stream()
                .map(MarginIndustrySummaryPO::getIndustryCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        List<MarginIndustryConstituentPO> constituents = marginTradingBasicService.listIndustryConstituents(
                tradeDate, industryCodes
        );
        Map<String, Map<LocalDate, BigDecimal>> dailyAmounts = loadDailyAmounts(constituents, tradeDate, tradeDate);
        Map<String, List<String>> symbolsByIndustry = groupSymbolsByIndustry(constituents);
        rows.forEach(row -> row.setTurnoverAmount(sumTurnover(
                symbolsByIndustry.get(row.getIndustryCode()), tradeDate, dailyAmounts
        )));
    }

    private void enrichIndustryHistoryTurnover(
            List<MarginIndustryHistoryPO> rows,
            LocalDate constituentDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<String> industryCodes = rows.stream()
                .map(MarginIndustryHistoryPO::getIndustryCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        List<MarginIndustryConstituentPO> constituents = marginTradingBasicService.listIndustryConstituents(
                constituentDate, industryCodes
        );
        Map<String, Map<LocalDate, BigDecimal>> dailyAmounts = loadDailyAmounts(constituents, startDate, endDate);
        Map<String, List<String>> symbolsByIndustry = groupSymbolsByIndustry(constituents);
        rows.forEach(row -> row.setTurnoverAmount(sumTurnover(
                symbolsByIndustry.get(row.getIndustryCode()), row.getTradeDate(), dailyAmounts
        )));
    }

    private Map<String, Map<LocalDate, BigDecimal>> loadDailyAmounts(
            List<MarginIndustryConstituentPO> constituents,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Set<String> symbols = constituents.stream()
                .map(MarginIndustryConstituentPO::getStockCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, Map<LocalDate, BigDecimal>> amountsBySymbolAndDate = new HashMap<>();
        if (symbols.isEmpty()) {
            return amountsBySymbolAndDate;
        }
        for (ClickHouseStockChartClient.DailyAmount amount : clickHouseStockChartClient.listDailyAmounts(
                List.copyOf(symbols), startDate, endDate
        )) {
            if (amount.symbol() == null || amount.tradeDate() == null || amount.amount() == null) {
                continue;
            }
            amountsBySymbolAndDate
                    .computeIfAbsent(amount.symbol(), ignored -> new HashMap<>())
                    .merge(amount.tradeDate(), amount.amount(), BigDecimal::add);
        }
        return amountsBySymbolAndDate;
    }

    private Map<String, List<String>> groupSymbolsByIndustry(List<MarginIndustryConstituentPO> constituents) {
        return constituents.stream()
                .filter(item -> item.getIndustryCode() != null && item.getStockCode() != null)
                .collect(Collectors.groupingBy(
                        MarginIndustryConstituentPO::getIndustryCode,
                        Collectors.mapping(MarginIndustryConstituentPO::getStockCode, Collectors.toList())
                ));
    }

    private BigDecimal sumTurnover(
            List<String> symbols,
            LocalDate tradeDate,
            Map<String, Map<LocalDate, BigDecimal>> dailyAmounts
    ) {
        if (symbols == null || tradeDate == null) {
            return BigDecimal.ZERO;
        }
        return symbols.stream()
                .map(dailyAmounts::get)
                .filter(amounts -> amounts != null)
                .map(amounts -> amounts.get(tradeDate))
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
