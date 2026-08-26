package cn.cccs7.cynexus.market.domain.service.factor.impl;

import cn.cccs7.cynexus.market.domain.entity.IndexDailyBarBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.domain.entity.factor.MarketEventFactorObservatoryBO;
import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.domain.repository.StockScreenerRepository;
import cn.cccs7.cynexus.market.domain.service.factor.MarketEventFactorObservatoryDomainService;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketPulseQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Domain composition of independently sourced market-event evidence. */
public final class MarketEventFactorObservatoryDomainServiceImpl implements MarketEventFactorObservatoryDomainService {
    private static final int DEFAULT_LIMIT = 40;
    private static final int MAX_LIMIT = 100;
    private static final BigDecimal DEFAULT_LARGE_MOVE_PCT = new BigDecimal("7");
    private static final StockScreenerQueryBO.PriceVolumePattern TECHNICAL_METRICS =
            new StockScreenerQueryBO.PriceVolumePattern(2, BigDecimal.ONE, null, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 5, new BigDecimal("100"), 1, BigDecimal.ZERO,
                    new BigDecimal("100"), "doji");

    private final MarketPulseQueryDomainService pulseService;
    private final StockScreenerRepository screenerRepository;
    private final MarketSentimentQueryDomainService sentimentService;
    private final MarketIndicatorQueryDomainService indicatorService;
    private final MarginTradingRepository marginRepository;

    public MarketEventFactorObservatoryDomainServiceImpl(MarketPulseQueryDomainService pulseService,
                                                           StockScreenerRepository screenerRepository,
                                                           MarketSentimentQueryDomainService sentimentService,
                                                           MarketIndicatorQueryDomainService indicatorService,
                                                           MarginTradingRepository marginRepository) {
        this.pulseService = pulseService;
        this.screenerRepository = screenerRepository;
        this.sentimentService = sentimentService;
        this.indicatorService = indicatorService;
        this.marginRepository = marginRepository;
    }

    @Override
    public MarketEventFactorObservatoryBO.Result observe(MarketEventFactorObservatoryBO.Query input) {
        Request request = normalize(input);
        MarketPulseQueryDomainService.LimitEmotionQuery limitEmotion = "LIMIT_UP".equals(request.cohort())
                ? pulseService.limitEmotion(request.requestedDate()) : null;
        Optional<StockScreenerQueryBO.Snapshot> eventSnapshot = Optional.empty();
        LocalDate eventDate = limitEmotion == null || limitEmotion.snapshot() == null ? null : limitEmotion.snapshot().getTradeDate();
        if (eventDate == null) {
            eventSnapshot = screenerRepository.findSnapshotOnOrBefore(request.requestedDate(), TECHNICAL_METRICS, 80);
            if (eventSnapshot.isEmpty()) return empty(request, "daily screener snapshot is unavailable");
            eventDate = eventSnapshot.get().tradeDate();
        }
        Optional<StockScreenerQueryBO.Snapshot> priorSnapshot = screenerRepository.findSnapshotOnOrBefore(eventDate.minusDays(1), TECHNICAL_METRICS, 80);
        Map<String, StockScreenerQueryBO.Row> priorRows = priorSnapshot.map(this::rowsByCode).orElseGet(Map::of);
        LocalDate priorDate = priorSnapshot.map(StockScreenerQueryBO.Snapshot::tradeDate).orElse(null);
        Map<String, StockScreenerQueryBO.Row> eventRows = eventSnapshot.map(this::rowsByCode).orElseGet(Map::of);
        List<Candidate> population = candidates(request, eventDate, eventRows, priorRows, limitEmotion);
        if (request.symbol() != null) population = population.stream().filter(item -> request.symbol().equals(item.code())).toList();
        List<Candidate> sample = population.stream().limit(request.limit()).toList();
        List<MarketIndicatorIndustrySnapshotBO> industries = indicatorService.latestIndustries(eventDate, 300);
        Map<String, MarketIndicatorIndustrySnapshotBO> industriesByKey = industries.stream()
                .filter(item -> item.getIndustryCode() != null || item.getIndustryName() != null)
                .flatMap(item -> Stream.of(item.getIndustryCode(), item.getIndustryName())
                        .filter(value -> value != null && !value.isBlank()).map(value -> Map.entry(value, item)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        Map<String, MarginStockDetailBO> marginStocks = marginStocks(eventDate);
        List<MarketEventFactorObservatoryBO.FactorFamily> marketFactors = marketFactors(eventDate, industries, population);
        List<MarketEventFactorObservatoryBO.Event> events = new ArrayList<>();
        for (Candidate candidate : sample) {
            events.add(event(candidate, priorRows.get(candidate.code()), priorDate, industriesByKey,
                    marginStocks.get(candidate.code()), eventDate));
        }
        List<String> warnings = new ArrayList<>();
        warnings.add("该结果是多源观察证据，不代表单一原因或交易结论。");
        warnings.add("事件日前技术因子使用上一可用日线快照；概念和风格成员关系是当前映射，不能视为历史归因。");
        if (priorDate == null) warnings.add("未找到事件日前日线快照，个股前置技术证据标记为不可用。");
        if (industries.isEmpty()) warnings.add("行业盘中/收盘快照不可用，行业因子不能用于归因。");
        return new MarketEventFactorObservatoryBO.Result(true, request.requestedDate(), eventDate, request.cohort(),
                request.minLargeMovePct(), population.size(), events.size(), marketFactors, events,
                screeningHints(population, priorRows), warnings, null);
    }

    private List<Candidate> candidates(Request request, LocalDate eventDate, Map<String, StockScreenerQueryBO.Row> rows,
                                       Map<String, StockScreenerQueryBO.Row> priorRows,
                                       MarketPulseQueryDomainService.LimitEmotionQuery limitEmotion) {
        if ("LIMIT_UP".equals(request.cohort())) {
            MarketPulseQueryDomainService.LimitEmotionQuery serving = limitEmotion == null ? pulseService.limitEmotion(eventDate) : limitEmotion;
            return serving.limitUp().stream().map(item -> candidate(item, priorRows.get(item.getCode())))
                    .sorted(Comparator.comparing(Candidate::changePct, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        }
        return rows.values().stream().filter(row -> row.changePct() != null && row.changePct().compareTo(request.minLargeMovePct()) >= 0)
                .map(row -> new Candidate(row.code(), row.name(), "LARGE_MOVE", row.changePct(), null, row))
                .sorted(Comparator.comparing(Candidate::changePct, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    private Candidate candidate(MarketLimitDailyStockBO item, StockScreenerQueryBO.Row row) {
        return new Candidate(item.getCode(), row == null ? item.getName() : row.name(), "LIMIT_UP", item.getChangePct(), item.getStreak(), row);
    }

    private MarketEventFactorObservatoryBO.Event event(Candidate candidate, StockScreenerQueryBO.Row prior,
                                                         LocalDate priorDate, Map<String, MarketIndicatorIndustrySnapshotBO> industries,
                                                         MarginStockDetailBO margin, LocalDate eventDate) {
        StockScreenerQueryBO.Row row = candidate.row();
        List<MarketEventFactorObservatoryBO.FactorFamily> factors = List.of(
                preEvent(prior, priorDate), industry(row, industries, eventDate), margin(margin, eventDate));
        return new MarketEventFactorObservatoryBO.Event(candidate.code(), candidate.name(), candidate.type(), candidate.changePct(), candidate.streak(),
                row == null ? null : row.thsIndustryCode(), row == null ? null : row.thsIndustryName(),
                row == null ? null : row.tdxIndustryCode(), row == null ? null : row.tdxIndustryName(),
                row == null ? List.of() : row.concepts().stream().map(StockScreenerQueryBO.SectorMembership::name).toList(),
                row == null ? List.of() : row.styles().stream().map(StockScreenerQueryBO.SectorMembership::name).toList(), factors);
    }

    private List<MarketEventFactorObservatoryBO.FactorFamily> marketFactors(LocalDate date,
                                                                               List<MarketIndicatorIndustrySnapshotBO> industries,
                                                                               List<Candidate> population) {
        List<MarketEventFactorObservatoryBO.FactorFamily> factors = new ArrayList<>();
        factors.add(marketRegime(date));
        factors.add(msi(date));
        factors.add(breadth(date));
        factors.add(indexPath(date));
        factors.add(industryProfile(date, industries, population));
        factors.add(intradayCoverage(date));
        factors.add(marginMarket(date));
        return factors;
    }

    private MarketEventFactorObservatoryBO.FactorFamily marketRegime(LocalDate date) {
        Map<String, Object> state = indicatorService.radarState(date, null);
        return family("market_regime", "market_state", "大盘状态", available(state.get("breadth")), date,
                "postgres.market_indicator", List.of(metric("state", "状态", state.get("marketState"), null),
                        metric("reason", "判定依据", state.get("stateReason"), null), metric("coverage", "覆盖股票数", state.get("coverage"), "只")),
                List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily msi(LocalDate date) {
        var result = sentimentService.msiIndex(date);
        MsiIndexDailyBO row = result.value();
        if (row == null) return unavailable("market_regime", "msi_9", "MSI 九因子", date, "postgres.msi_index_daily");
        return family("market_regime", "msi_9", "MSI 九因子", "AVAILABLE", row.getTradeDate(), row.getSource(), List.of(
                metric("vol", "波动", row.getVolScore(), "分"), metric("turnover", "成交活跃", row.getTurnoverScore(), "分"),
                metric("priceStrength", "价格强度", row.getPriceStrengthScore(), "分"), metric("riskAppetite", "风险偏好", row.getRiskAppetiteScore(), "分"),
                metric("breadth", "市场广度", row.getBreadthScore(), "分"), metric("limitEmotion", "涨停情绪", row.getLimitEmotionScore(), "分"),
                metric("profitEffect", "赚钱效应", row.getProfitEffectScore(), "分"), metric("sectorBreadth", "行业广度", row.getSectorBreadthScore(), "分"),
                metric("styleRisk", "风格风险", row.getStyleRiskScore(), "分"), metric("composite", "综合分", row.getCompositeScore(), "分")),
                List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily breadth(LocalDate date) {
        MaCountDailyBO row = sentimentService.maCount(date).value();
        if (row == null) return unavailable("market_regime", "breadth_new_high_low", "广度、新高与新低", date, "postgres.market_sentiment_ma_count_daily");
        return family("market_regime", "breadth_new_high_low", "广度、新高与新低", "AVAILABLE", row.getTradeDate(), row.getSource(), List.of(
                metric("aboveMa20", "站上 MA20", row.getPctMa20(), "%"), metric("aboveMa60", "站上 MA60", row.getPctMa60(), "%"),
                metric("aboveBoth", "同时站上 MA20/60", row.getPctBoth(), "%"), metric("newLow60d", "60 日新低", row.getNewLow60dPct(), "%"),
                metric("newHigh252d", "252 日新高", row.getNewHigh252dPct(), "%"), metric("advancing", "上涨家数占比", row.getAdvancingPct(), "%")),
                List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily indexPath(LocalDate date) {
        List<MarketEventFactorObservatoryBO.Metric> values = new ArrayList<>();
        for (String code : List.of("000001", "399001", "399006")) {
            List<IndexDailyBarBO> rows = sentimentService.indexDaily(code, date.minusDays(65), date).items();
            if (rows.size() >= 2) values.add(metric(code, sentimentService.indexName(code) + "区间涨幅", percent(rows.getFirst().getClose(), rows.getLast().getClose()), "%"));
        }
        return values.isEmpty() ? unavailable("market_regime", "index_path", "指数路径", date, "postgres.index_daily")
                : family("market_regime", "index_path", "指数路径", "AVAILABLE", date, "postgres.index_daily", values, List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily industryProfile(LocalDate date, List<MarketIndicatorIndustrySnapshotBO> industries,
                                                                          List<Candidate> population) {
        if (industries.isEmpty()) return unavailable("industry_regime", "industry_state", "行业状态", date, "postgres.market_indicator_industry_snapshot");
        Map<String, Long> concentration = population.stream().map(Candidate::row).filter(row -> row != null)
                .map(row -> row.thsIndustryName() == null ? row.industry() : row.thsIndustryName()).filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<MarketEventFactorObservatoryBO.Metric> metrics = concentration.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5).map(item -> metric("cohort." + item.getKey(), "事件池行业：" + item.getKey(), item.getValue(), "只")).toList();
        return family("industry_regime", "industry_state", "行业状态", "AVAILABLE", date, "postgres.market_indicator_industry_snapshot", metrics,
                List.of(), List.of(), List.of("行业快照覆盖 " + industries.size() + " 个行业；行业集中度仅描述样本分布。"));
    }

    private MarketEventFactorObservatoryBO.FactorFamily intradayCoverage(LocalDate date) {
        Map<String, Object> state = indicatorService.tradeReviewMarketStateStatus(date);
        return family("intraday", "intraday_industry_coverage", "日内行业与市场回放覆盖", text(state.get("dataStatus"), "UNAVAILABLE").toUpperCase(Locale.ROOT), date,
                text(state.get("source"), "trade_review_materialized_manifest"), List.of(metric("status", "状态", state.get("dataStatus"), null)),
                List.of(), List.of(), strings(state.get("qualityReasons")));
    }

    private MarketEventFactorObservatoryBO.FactorFamily marginMarket(LocalDate date) {
        LocalDate serving = marginRepository.findLatestTradeDate(date).orElse(null);
        if (serving == null) return unavailable("cross_market", "margin_market", "两融市场", date, "postgres.margin_trading");
        List<MarginMarketSummaryBO> values = marginRepository.listMarketSummaryByTradeDate(serving);
        BigDecimal balance = values.stream().map(MarginMarketSummaryBO::getMarginBalance).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return family("cross_market", "margin_market", "两融市场", "AVAILABLE", serving, "postgres.margin_trading", List.of(
                metric("marginBalance", "两融余额", balance, "元"), metric("exchangeCount", "交易所覆盖", values.size(), "个")),
                List.of(), List.of(), serving.equals(date) ? List.of() : List.of("两融数据按最近可用交易日提供。"));
    }

    private MarketEventFactorObservatoryBO.FactorFamily preEvent(StockScreenerQueryBO.Row row, LocalDate date) {
        if (row == null) return unavailable("pre_event", "daily_technical", "事件日前技术与资金", date, "daily screener snapshot");
        StockScreenerQueryBO.PriceVolumePatternMetrics pattern = row.priceVolumePattern();
        List<MarketEventFactorObservatoryBO.Metric> metrics = new ArrayList<>(List.of(
                metric("return5d", "5 日涨幅", row.returnPct5d(), "%"), metric("return10d", "10 日涨幅", row.returnPct10d(), "%"),
                metric("return20d", "20 日代理（MA20 偏离）", pattern == null ? null : pattern.closeAboveMa20Pct(), "%"),
                metric("return60d", "60 日涨幅", row.returnPct60d(), "%"), metric("return252d", "252 日涨幅", row.returnPct252d(), "%"),
                metric("mainNet5d", "5 日主力净流入", row.mainNet5d(), "元"), metric("turnover", "换手率", row.turnoverPct(), "%"), metric("volumeRatio", "量比", row.volumeRatio(), null)));
        if (pattern != null) metrics.addAll(List.of(metric("contractionDays", "连续缩量天数", pattern.observedContractionDays(), "日"),
                metric("volumeVsMa5", "成交量/MA5", pattern.volumeVsMa5Multiple(), "倍"), metric("breakoutVolume", "突破放量", pattern.breakoutVolumeMultiple(), "倍"),
                metric("breakoutHighDays", "突破近高", pattern.breakoutHighDays(), "日"), metric("candles", "K 线形态", pattern.candlestickPatterns() == null ? null : String.join("、", pattern.candlestickPatterns()), null)));
        return family("pre_event", "daily_technical", "事件日前技术与资金", "AVAILABLE", date, "daily screener snapshot", metrics,
                List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily industry(StockScreenerQueryBO.Row row,
                                                                   Map<String, MarketIndicatorIndustrySnapshotBO> industries, LocalDate date) {
        if (row == null) return unavailable("industry_regime", "event_industry", "所属行业状态", date, "postgres.market_indicator_industry_snapshot");
        MarketIndicatorIndustrySnapshotBO item = first(industries, row.thsIndustryCode(), row.thsIndustryName(), row.tdxIndustryCode(), row.tdxIndustryName(), row.industry());
        if (item == null) return unavailable("industry_regime", "event_industry", "所属行业状态", date, "postgres.market_indicator_industry_snapshot");
        return family("industry_regime", "event_industry", "所属行业状态", "AVAILABLE", item.getTradeDate(), item.getSource(), List.of(
                metric("industry", "行业", item.getIndustryName(), null), metric("advancing", "上涨家数", item.getAdvancingCount(), "只"),
                metric("declining", "下跌家数", item.getDecliningCount(), "只"), metric("limitUp", "涨停家数", item.getLimitUpCount(), "只"),
                metric("firstLimitUpAt", "首板出现时间", item.getFirstLimitUpAt(), null), metric("rapidRise", "快速上涨", item.getRapidRiseCount(), "只")),
                List.of(), List.of(), List.of());
    }

    private MarketEventFactorObservatoryBO.FactorFamily margin(MarginStockDetailBO row, LocalDate date) {
        if (row == null) return unavailable("cross_market", "margin_stock", "个股两融", date, "postgres.margin_trading_stock");
        return family("cross_market", "margin_stock", "个股两融", "AVAILABLE", row.getTradeDate(), row.getSource(), List.of(
                metric("marginBalance", "两融余额", row.getMarginBalance(), "元"), metric("financingBuy", "融资买入", row.getFinancingBuyAmount(), "元"),
                metric("shortBalance", "融券余额", row.getSecuritiesLendingBalanceAmount(), "元")), List.of(), List.of(), List.of());
    }

    private Map<String, MarginStockDetailBO> marginStocks(LocalDate date) {
        LocalDate serving = marginRepository.findLatestTradeDate(date).orElse(null);
        if (serving == null) return Map.of();
        return marginRepository.listStocks(serving, null, null, "margin_balance", "desc", 0, 5000).stream()
                .filter(item -> item.getStockCode() != null).collect(Collectors.toMap(MarginStockDetailBO::getStockCode, Function.identity(), (left, right) -> left));
    }

    private List<MarketEventFactorObservatoryBO.ScreeningHint> screeningHints(List<Candidate> population,
                                                                                 Map<String, StockScreenerQueryBO.Row> priorRows) {
        long available = population.stream().map(Candidate::code).filter(priorRows::containsKey).count();
        if (available == 0) return List.of();
        return List.of(new MarketEventFactorObservatoryBO.ScreeningHint("minCloseAboveMa20Pct", ">=", "0", "收盘不低于 MA20", "事件日前样本技术快照"),
                new MarketEventFactorObservatoryBO.ScreeningHint("minVolumeVsMa5Multiple", ">=", "1", "成交量不低于 MA5", "事件日前样本技术快照"));
    }

    private Map<String, StockScreenerQueryBO.Row> rowsByCode(StockScreenerQueryBO.Snapshot snapshot) {
        return snapshot.rows().stream().filter(row -> row.code() != null).collect(Collectors.toMap(StockScreenerQueryBO.Row::code, Function.identity(), (left, right) -> left));
    }

    private MarketEventFactorObservatoryBO.Result empty(Request request, String error) {
        return new MarketEventFactorObservatoryBO.Result(false, request.requestedDate(), null, request.cohort(), request.minLargeMovePct(), 0, 0,
                List.of(), List.of(), List.of(), List.of("无法读取日线快照，未生成任何因子结论。"), error);
    }

    private Request normalize(MarketEventFactorObservatoryBO.Query query) {
        LocalDate date;
        try { date = query == null || query.tradeDate() == null || query.tradeDate().isBlank() ? LocalDate.now() : LocalDate.parse(query.tradeDate()); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("tradeDate must be yyyy-MM-dd", exception); }
        String cohort = query == null || query.cohort() == null ? "LIMIT_UP" : query.cohort().trim().toUpperCase(Locale.ROOT);
        if (!List.of("LIMIT_UP", "LARGE_MOVE").contains(cohort)) throw new IllegalArgumentException("cohort must be LIMIT_UP or LARGE_MOVE");
        int limit = query == null || query.limit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, query.limit()));
        BigDecimal threshold = query == null || query.minLargeMovePct() == null ? DEFAULT_LARGE_MOVE_PCT : query.minLargeMovePct();
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(new BigDecimal("30")) > 0) throw new IllegalArgumentException("minLargeMovePct must be between 0 and 30");
        String symbol = query == null || query.symbol() == null || query.symbol().isBlank() ? null : query.symbol().trim();
        return new Request(date, cohort, threshold, limit, symbol);
    }

    private MarketEventFactorObservatoryBO.FactorFamily unavailable(String stage, String code, String label, LocalDate date, String source) {
        return family(stage, code, label, "UNAVAILABLE", date, source, List.of(), List.of(), List.of(), List.of("该因子源在所选日期没有可用数据。"));
    }
    private MarketEventFactorObservatoryBO.FactorFamily family(String stage, String code, String label, String status, LocalDate date, String source,
                                                                  List<MarketEventFactorObservatoryBO.Metric> metrics, List<String> support, List<String> counter, List<String> warnings) {
        return new MarketEventFactorObservatoryBO.FactorFamily(stage, code, label, status, date, source, metrics, support, counter, warnings);
    }
    private MarketEventFactorObservatoryBO.Metric metric(String key, String label, Object value, String unit) {
        return new MarketEventFactorObservatoryBO.Metric(key, label, value == null ? null : String.valueOf(value), unit);
    }
    private String available(Object value) { return value == null ? "UNAVAILABLE" : "AVAILABLE"; }
    private String text(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private List<String> strings(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of(); }
    private BigDecimal percent(BigDecimal base, BigDecimal current) {
        if (base == null || current == null || base.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(base).multiply(new BigDecimal("100")).divide(base, 2, RoundingMode.HALF_UP);
    }
    private MarketIndicatorIndustrySnapshotBO first(Map<String, MarketIndicatorIndustrySnapshotBO> values, String... keys) {
        for (String key : keys) if (key != null && values.containsKey(key)) return values.get(key);
        return null;
    }
    private record Request(LocalDate requestedDate, String cohort, BigDecimal minLargeMovePct, int limit, String symbol) {}
    private record Candidate(String code, String name, String type, BigDecimal changePct, Integer streak, StockScreenerQueryBO.Row row) {}
}
