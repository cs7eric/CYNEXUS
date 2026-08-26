package cn.cccs7.cynexus.market.domain.service.screener.impl;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.repository.StockScreenerRepository;
import cn.cccs7.cynexus.market.domain.screener.StockScreenerDomainPolicy;
import cn.cccs7.cynexus.market.domain.screener.StockScreenerDataQualityPolicy;
import cn.cccs7.cynexus.market.domain.service.screener.StockScreenerQueryDomainService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class StockScreenerQueryDomainServiceImpl implements StockScreenerQueryDomainService {

    private final StockScreenerRepository repository;
    private final StockScreenerDomainPolicy policy = new StockScreenerDomainPolicy();
    private final StockScreenerDataQualityPolicy dataQualityPolicy = new StockScreenerDataQualityPolicy();

    public StockScreenerQueryDomainServiceImpl(StockScreenerRepository repository) {
        this.repository = repository;
    }

    @Override
    public StockScreenerQueryBO.Result screen(StockScreenerQueryBO.QueryInput input) {
        StockScreenerQueryBO.Query query = policy.normalize(input);
        return repository.findSnapshotOnOrBefore(query.requestedTradeDate(), query.priceVolumePattern(),
                        policy.dailyQuoteHistorySessions(query))
                .map(snapshot -> result(snapshot, query))
                .orElseGet(() -> new StockScreenerQueryBO.Result(false, query.requestedTradeDate(), null,
                        query.universe(), query.excludeSt(), query.excludeChiNext(), query.excludeStar(), query.industry(), query.tdxIndustryCode(), query.thsIndustryCode(), query.conceptSectorId(), query.styleSectorId(), query.board(), query.sort(), query.sortDir(), query.page(),
                        0, 0, "clickhouse.daily_raw + postgres.sec_stock_basic + postgres.a_stock_fund_flow_daily + postgres.ths_industry_constituent_snapshot + postgres.sec_stock_sector", java.util.List.of(),
                        java.util.List.of("No daily snapshot is available for the requested scope."), "daily screener data is empty", null));
    }

    @Override
    public StockScreenerQueryBO.ClassificationOptions listClassificationOptions(String tradeDate) {
        LocalDate requestedDate;
        try {
            requestedDate = tradeDate == null || tradeDate.isBlank() ? LocalDate.now() : LocalDate.parse(tradeDate.trim());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("tradeDate must be yyyy-MM-dd", exception);
        }
        return repository.findClassificationOptionsOnOrBefore(requestedDate);
    }

    private StockScreenerQueryBO.Result result(StockScreenerQueryBO.Snapshot snapshot, StockScreenerQueryBO.Query query) {
        java.util.List<StockScreenerQueryBO.Row> universe = snapshot.rows().stream()
                .filter(row -> "ALL".equals(query.universe()) || query.universe().equalsIgnoreCase(row.market()))
                .toList();
        java.util.List<StockScreenerQueryBO.Row> matched = universe.stream()
                .filter(row -> policy.matches(row, query))
                .sorted(policy.comparator(query))
                .toList();
        int from = Math.min(query.page().offset(), matched.size());
        int to = Math.min(from + query.page().size(), matched.size());
        StockScreenerQueryBO.DataQuality dataQuality = dataQualityPolicy.assess(snapshot);
        return new StockScreenerQueryBO.Result(true, query.requestedTradeDate(), snapshot.tradeDate(),
                query.universe(), query.excludeSt(), query.excludeChiNext(), query.excludeStar(), query.industry(), query.tdxIndustryCode(), query.thsIndustryCode(), query.conceptSectorId(), query.styleSectorId(), query.board(), query.sort(), query.sortDir(), query.page(),
                universe.size(), matched.size(), snapshot.source(), matched.subList(from, to),
                warnings(dataQuality, query), null, dataQuality);
    }

    private java.util.List<String> warnings(StockScreenerQueryBO.DataQuality dataQuality, StockScreenerQueryBO.Query query) {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        warnings.add("Daily close snapshot. Industry and fund-flow values must align to the actual snapshot date; unavailable aligned values are not backfilled. Concepts and styles are current memberships, not historical snapshots. PE is partial coverage and is not a complete financial screen.");
        if (query.priceVolumePattern() != null && query.priceVolumePattern().enabled()) {
            warnings.add("Technical and candlestick conditions are evaluated only when the durable daily OHLCV fact is present on the actual snapshot date; stale facts are excluded.");
        }
        warnings.addAll(dataQuality.warnings());
        if ("STALE".equals(dataQuality.status())) warnings.add("Daily snapshot is stale by " + dataQuality.freshnessLagDays() + " calendar days.");
        return java.util.List.copyOf(warnings);
    }
}
