package cn.cccs7.cynexus.market.domain.service.margin.impl;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.entity.margin.MarginTradingQueryBO;
import cn.cccs7.cynexus.market.domain.margin.MarginTradingDomainPolicy;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.domain.service.margin.MarginTradingQueryDomainService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarginTradingQueryDomainServiceImpl implements MarginTradingQueryDomainService {

    private final MarginTradingRepository repository;
    private final MarginTradingDomainPolicy policy = new MarginTradingDomainPolicy();

    public MarginTradingQueryDomainServiceImpl(MarginTradingRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarginTradingQueryBO.Latest latest(String tradeDate, int top) {
        LocalDate requested = policy.parseDate(tradeDate).orElse(null);
        LocalDate serving = repository.findLatestTradeDate(requested).orElse(null);
        if (serving == null) {
            return new MarginTradingQueryBO.Latest(false, requested, null, false, null, null,
                    List.of(), null, List.of(), 0, "margin trading data is empty");
        }
        int safeTop = policy.clampLimit(top, MarginTradingDomainPolicy.DEFAULT_TOP_STOCK_LIMIT);
        List<MarginMarketSummaryBO> summaries = repository.listMarketSummaryByTradeDate(serving);
        MarginAccountStatsBO account = repository.findAccountStatsByTradeDate(serving).orElse(null);
        List<MarginStockDetailBO> topStocks = repository.listStocks(serving, null, null,
                "margin_balance", "desc", 0, safeTop);
        var metadata = policy.servingMetadata(summaries, account, topStocks);
        return new MarginTradingQueryBO.Latest(true, requested, serving,
                requested != null && !requested.equals(serving),
                metadata.source(), metadata.updatedAt(),
                summaries, account, topStocks, repository.countStocks(serving, null, null), null);
    }

    @Override
    public MarginTradingQueryBO.History summaryHistory(String start, String end) {
        LocalDate endDate = policy.parseDate(end).orElse(LocalDate.now());
        LocalDate startDate = policy.parseDate(start)
                .orElse(endDate.minusDays(MarginTradingDomainPolicy.DEFAULT_HISTORY_DAYS - 1L));
        if (startDate.isAfter(endDate)) {
            LocalDate previousStart = startDate;
            startDate = endDate;
            endDate = previousStart;
        }
        return new MarginTradingQueryBO.History(startDate, endDate,
                repository.listMarketSummaryHistory(startDate, endDate));
    }

    @Override
    public MarginTradingQueryBO.Stocks stocks(String tradeDate, String exchange, String industryCode,
                                              String sort, String sortDir, Integer page, Integer pageSize, int limit) {
        LocalDate requested = policy.parseDate(tradeDate).orElse(null);
        LocalDate serving = repository.findLatestTradeDate(requested).orElse(null);
        MarginTradingQueryBO.Page slice = page(page, pageSize == null ? limit : pageSize,
                MarginTradingDomainPolicy.DEFAULT_STOCK_LIMIT, MarginTradingDomainPolicy.MAX_STOCK_LIMIT);
        String normalizedExchange = policy.normalizeExchange(exchange);
        String normalizedIndustry = policy.normalizeIndustryCode(industryCode);
        String normalizedSort = policy.normalizeStockSort(sort);
        String normalizedSortDir = policy.normalizeSortDirection(sortDir);
        if (serving == null) {
            return new MarginTradingQueryBO.Stocks(false, requested, requested, normalizedExchange,
                    normalizedIndustry, normalizedSort, normalizedSortDir, slice, 0, List.of(),
                    "margin stock detail data is empty");
        }
        List<MarginStockDetailBO> items = repository.listStocks(serving, normalizedExchange, normalizedIndustry,
                normalizedSort, normalizedSortDir, slice.offset(), slice.size());
        return new MarginTradingQueryBO.Stocks(true, requested, serving, normalizedExchange, normalizedIndustry,
                normalizedSort, normalizedSortDir, slice,
                repository.countStocks(serving, normalizedExchange, normalizedIndustry), items, null);
    }

    @Override
    public MarginTradingQueryBO.StockHistory stockHistory(String stockCode, int limit) {
        String normalizedCode = policy.normalizeStockCode(stockCode);
        return new MarginTradingQueryBO.StockHistory(normalizedCode,
                repository.listStockHistory(normalizedCode,
                        policy.clampLimit(limit, MarginTradingDomainPolicy.DEFAULT_HISTORY_DAYS)));
    }

    @Override
    public MarginTradingQueryBO.Industries industries(String tradeDate, String sort, String sortDir,
                                                      Integer page, Integer pageSize) {
        LocalDate requested = policy.parseDate(tradeDate).orElse(null);
        LocalDate serving = repository.findLatestTradeDate(requested).orElse(null);
        MarginTradingQueryBO.Page slice = page(page, pageSize,
                MarginTradingDomainPolicy.DEFAULT_RANKING_PAGE_SIZE, MarginTradingDomainPolicy.MAX_STOCK_LIMIT);
        String normalizedSort = policy.normalizeIndustrySort(sort);
        String normalizedSortDir = policy.normalizeSortDirection(sortDir);
        if (serving == null) {
            return new MarginTradingQueryBO.Industries(false, requested, requested, null, normalizedSort,
                    normalizedSortDir, slice, 0, List.of(), "margin stock detail data is empty");
        }
        List<MarginIndustrySummaryBO> items = repository.listIndustrySummaries(serving, normalizedSort,
                normalizedSortDir, slice.offset(), slice.size());
        return new MarginTradingQueryBO.Industries(true, requested, serving,
                items.isEmpty() ? null : items.getFirst().getConstituentSnapshotDate(), normalizedSort,
                normalizedSortDir, slice, repository.countIndustrySummaries(serving), items, null);
    }

    @Override
    public MarginTradingQueryBO.IndustryHistory industryHistory(String industryCode, String tradeDate, int limit) {
        LocalDate requested = policy.parseDate(tradeDate).orElse(null);
        LocalDate serving = repository.findLatestTradeDate(requested).orElse(null);
        String normalizedIndustry = policy.normalizeIndustryCode(industryCode);
        int safeLimit = policy.clampLimit(limit, MarginTradingDomainPolicy.DEFAULT_HISTORY_DAYS);
        if (serving == null || normalizedIndustry == null || normalizedIndustry.isBlank()) {
            return new MarginTradingQueryBO.IndustryHistory(false, normalizedIndustry, serving, List.of(),
                    "margin industry history data is empty");
        }
        return new MarginTradingQueryBO.IndustryHistory(true, normalizedIndustry, serving,
                repository.listIndustryHistory(serving, normalizedIndustry, safeLimit), null);
    }

    private MarginTradingQueryBO.Page page(Integer number, Integer size, int defaultSize, int maxSize) {
        int safeNumber = number == null || number <= 0 ? 1 : number;
        int requestedSize = size == null || size <= 0 ? defaultSize : size;
        int safeSize = Math.max(1, Math.min(requestedSize, maxSize));
        return new MarginTradingQueryBO.Page(safeNumber, safeSize, (safeNumber - 1) * safeSize);
    }
}
