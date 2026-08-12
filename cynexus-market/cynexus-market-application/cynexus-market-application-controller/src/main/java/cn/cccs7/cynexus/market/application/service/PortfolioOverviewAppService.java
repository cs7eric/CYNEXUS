package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioAccountDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioAccountUpdateRequest;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountDomainService;
import cn.cccs7.cynexus.market.api.dto.PortfolioLedgerWarningDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPositionDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformanceDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformanceContributionDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformancePointDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealizedPnlBaselineRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioInstrumentType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceGranularity;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPricePointBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIntradayPricePointBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealtimePriceGateway;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioOverviewAppService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioOverviewAppService.class);
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 0);
    private final PortfolioTradeQueryRepository tradeRepository;
    private final PortfolioRealizedPnlBaselineRepository realizedPnlBaselineRepository;
    private final PortfolioAccountDomainService accountDomainService;
    private final PortfolioLedgerDomainService ledgerService;
    private final StockChartQueryDomainService stockChartQueryDomainService;
    private final PortfolioRealtimePriceGateway realtimePriceGateway;
    private final PortfolioPerformanceDomainService performanceDomainService;
    private final Clock clock;

    @Autowired
    public PortfolioOverviewAppService(PortfolioTradeQueryRepository tradeRepository,
                                       PortfolioRealizedPnlBaselineRepository realizedPnlBaselineRepository,
                                       PortfolioAccountDomainService accountDomainService,
                                       PortfolioLedgerDomainService ledgerService,
                                       StockChartQueryDomainService stockChartQueryDomainService,
                                       PortfolioRealtimePriceGateway realtimePriceGateway,
                                       PortfolioPerformanceDomainService performanceDomainService) {
        this(tradeRepository, realizedPnlBaselineRepository, accountDomainService, ledgerService,
                stockChartQueryDomainService, realtimePriceGateway, performanceDomainService,
                Clock.system(SHANGHAI));
    }

    PortfolioOverviewAppService(PortfolioTradeQueryRepository tradeRepository,
                                PortfolioRealizedPnlBaselineRepository realizedPnlBaselineRepository,
                                PortfolioAccountDomainService accountDomainService,
                                PortfolioLedgerDomainService ledgerService,
                                StockChartQueryDomainService stockChartQueryDomainService,
                                PortfolioRealtimePriceGateway realtimePriceGateway,
                                PortfolioPerformanceDomainService performanceDomainService,
                                Clock clock) {
        this.tradeRepository = tradeRepository;
        this.realizedPnlBaselineRepository = realizedPnlBaselineRepository;
        this.accountDomainService = accountDomainService;
        this.ledgerService = ledgerService;
        this.stockChartQueryDomainService = stockChartQueryDomainService;
        this.realtimePriceGateway = realtimePriceGateway;
        this.performanceDomainService = performanceDomainService;
        this.clock = clock == null ? Clock.system(SHANGHAI) : clock;
    }

    public PortfolioOverviewDTO overview() {
        LocalDate today = today();
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        var baselines = realizedPnlBaselineRepository.list(PRINCIPAL, TENANT);
        var preliminary = ledgerService.applyRealizedPnlBaselines(
                ledgerService.calculate(trades, Map.of()), baselines);
        List<PortfolioPositionBO> quotePositions = quotePositions(trades, preliminary.positions());
        Map<String, BigDecimal> latestPrices = loadLatestPrices(quotePositions);
        Map<String, BigDecimal> previousClosePrices = realtimePriceGateway.previousClosePrices(quotePositions);
        Map<String, LocalDate> realtimePriceDates = new HashMap<>(realtimePriceGateway.latestPriceDates(quotePositions));
        PortfolioLedgerResultBO result = ledgerService.applyRealizedPnlBaselines(
                ledgerService.calculate(trades, latestPrices), baselines);
        // Today's P&L must include trades that were fully closed today as well as
        // still-open positions. Build the quote series from the complete active
        // trade universe; the holdings table still filters zero-quantity positions.
        Map<String, List<PortfolioPricePointBO>> priceSeries = loadPortfolioPriceSeries(trades);
        Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries =
                loadPortfolioIntradayPriceSeries(trades, today);
        Map<String, BigDecimal> latestIntradayPrices = latestIntradayPrices(intradayPriceSeries);
        latestPrices.putAll(latestIntradayPrices);
        latestIntradayPrices.keySet().forEach(symbol -> realtimePriceDates.put(symbol, today));
        Map<String, BigDecimal> todayPnlBySymbol = isBeforeMarketClose()
                ? performanceDomainService.calculateTodayPnlBySymbol(
                        trades, priceSeries, latestPrices, previousClosePrices, realtimePriceDates,
                        intradayPriceSeries, today)
                : performanceDomainService.calculateTodayPnlBySymbol(
                        trades, priceSeries, latestPrices, previousClosePrices, realtimePriceDates,
                        intradayPriceSeries, today);
        BigDecimal costBasis = sum(result.positions(), PortfolioPositionBO::costBasis);
        BigDecimal marketValue = sum(result.positions(), PortfolioPositionBO::marketValue);
        PortfolioAccountBO account = accountDomainService.get(PRINCIPAL, TENANT);
        BigDecimal todayPnl = todayPnlBySymbol.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal accumulatedPnl = accumulatedPnlSinceAccountBaseline(
                account, trades, priceSeries, baselines, today, todayPnl);
        BigDecimal availableCash = accountDomainService.availableCash(account, marketValue, accumulatedPnl);
        BigDecimal totalAssets = accountDomainService.totalAssets(account, marketValue, accumulatedPnl);
        Map<String, String> securityNames = loadSecurityNames(result.positions());
        return PortfolioOverviewDTO.builder()
                .positions(result.positions().stream().map(position -> toPosition(position, securityNames, todayPnlBySymbol)).toList())
                .dataQualityWarnings(result.warnings().stream().map(warning -> PortfolioLedgerWarningDTO.builder()
                        .code(warning.code()).symbol(warning.symbol())
                        .unmatchedQuantity(warning.unmatchedQuantity()).message(warning.message()).build()).toList())
                .costBasis(costBasis).marketValue(marketValue)
                .availableCash(availableCash).totalAssets(totalAssets)
                .accountUpdatedAt(account.updatedAt())
                .realizedPnl(result.realizedPnl()).unrealizedPnl(result.unrealizedPnl()).totalPnl(result.totalPnl())
                .todayPnl(todayPnl)
                .tradeCount(trades.size())
                .valuationStatus(ledgerService.valuationStatus(trades, result, latestPrices).name()).build();
    }

    public PortfolioAccountDTO account() {
        PortfolioOverviewDTO overview = overview();
        return PortfolioAccountDTO.builder()
                .totalAssets(overview.getTotalAssets())
                .availableCash(overview.getAvailableCash())
                .updatedAt(overview.getAccountUpdatedAt()).build();
    }

    public PortfolioAccountDTO updateAccount(PortfolioAccountUpdateRequest request) {
        if (request == null) throw new IllegalArgumentException("portfolio account update is required");
        accountDomainService.updateTotalAssets(PRINCIPAL, TENANT, request.getTotalAssets());
        return account();
    }

    public List<PortfolioTradeDTO> trades(String symbol) {
        return tradeRepository.list(PRINCIPAL, TENANT, symbol).stream().map(trade -> PortfolioTradeDTO.builder()
                .id(trade.id()).symbol(trade.symbol()).securityName(trade.securityName()).instrumentType(trade.instrumentType().name()).side(trade.side().name())
                .tradedAt(trade.tradedAt()).quantity(trade.quantity()).price(trade.price())
                .totalFee(trade.totalFee()).status(trade.status().name()).build()).toList();
    }

    public PortfolioPerformanceDTO performance(String range, String fromValue, String toValue) {
        LocalDate today = today();
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        // The realized-P&L baseline is an accounting calibration point. It must
        // not truncate the performance window: weekly/monthly/yearly views
        // aggregate every available trading-day point inside the requested
        // window.
        PerformanceRange requested = resolveRange(range, fromValue, toValue);
        var openPositions = ledgerService.calculate(trades, Map.of()).positions();
        List<PortfolioPositionBO> quotePositions = quotePositions(trades, openPositions);
        Map<String, BigDecimal> realtimePrices = new HashMap<>(realtimePriceGateway.latestPrices(quotePositions));
        Map<String, BigDecimal> previousClosePrices = realtimePriceGateway.previousClosePrices(quotePositions);
        Map<String, LocalDate> realtimePriceDates = new HashMap<>(realtimePriceGateway.latestPriceDates(quotePositions));
        Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries =
                loadPortfolioIntradayPriceSeries(trades, today);
        Map<String, BigDecimal> latestIntradayPrices = latestIntradayPrices(intradayPriceSeries);
        realtimePrices.putAll(latestIntradayPrices);
        latestIntradayPrices.keySet().forEach(symbol -> realtimePriceDates.put(symbol, today));
        Map<String, PortfolioInstrumentType> instruments = trades.stream()
                .collect(Collectors.toMap(cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO::symbol,
                        cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO::instrumentType,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, List<PortfolioPricePointBO>> prices = new LinkedHashMap<>();
        for (Map.Entry<String, PortfolioInstrumentType> entry : instruments.entrySet()) {
            try {
                var kline = stockChartQueryDomainService.klines(entry.getValue().chartTargetType(), entry.getKey(), "1d", "qfq");
                List<PortfolioPricePointBO> series = kline.items().stream()
                        .filter(bar -> bar.getTradeDate() != null && bar.getClose() != null)
                        .map(bar -> new PortfolioPricePointBO(bar.getTradeDate(), bar.getClose()))
                        .toList();
                BigDecimal realtimePrice = realtimePrices.get(entry.getKey());
                boolean hasFreshRealtimePrice = today.equals(realtimePriceDates.get(entry.getKey()));
                // A fresh quote remains the best value for today's point after
                // the close as well. Keeping a stale kline value here made a
                // weekly/monthly/yearly aggregate briefly show the live value
                // and then revert on the next refresh.
                if (realtimePrice != null && hasFreshRealtimePrice) {
                    series = series.stream().filter(point -> !point.tradeDate().equals(today)).collect(Collectors.toCollection(java.util.ArrayList::new));
                    series.add(new PortfolioPricePointBO(today, realtimePrice));
                } else if (series.stream().noneMatch(point -> point.tradeDate().equals(today))) {
                    if (realtimePrice != null && hasFreshRealtimePrice) {
                        series = appendPricePoint(series, today, realtimePrice);
                        prices.put(entry.getKey(), series);
                        continue;
                    }
                    PortfolioPricePointBO lastHistoricalPrice = series.stream().filter(point -> point.tradeDate().isBefore(today))
                            .reduce((left, right) -> right).orElse(null);
                    if (lastHistoricalPrice != null) series = appendPricePoint(series, today, lastHistoricalPrice.close());
                }
                prices.put(entry.getKey(), series);
            } catch (RuntimeException exception) {
                log.warn("Unable to load portfolio performance prices for symbol={}", entry.getKey(), exception);
                prices.put(entry.getKey(), List.of());
            }
        }
        PortfolioAccountBO account = accountDomainService.get(PRINCIPAL, TENANT);
        var currentLedger = ledgerService.calculate(trades, realtimePrices);
        BigDecimal currentMarketValue = sum(currentLedger.positions(), PortfolioPositionBO::marketValue);
        Map<String, BigDecimal> todayPnlBySymbol = performanceDomainService.calculateTodayPnlBySymbol(
                trades, prices, realtimePrices, previousClosePrices, realtimePriceDates, intradayPriceSeries, today);
        BigDecimal todayPnl = todayPnlBySymbol.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal accumulatedPnl = accumulatedPnlSinceAccountBaseline(
                account, trades, prices, realizedPnlBaselineRepository.list(PRINCIPAL, TENANT), today, todayPnl);
        BigDecimal currentTotalAssets = accountDomainService.totalAssets(account, currentMarketValue, accumulatedPnl);
        PortfolioPerformanceResultBO result = performanceDomainService.calculate(
                trades, prices, realizedPnlBaselineRepository.list(PRINCIPAL, TENANT), currentTotalAssets,
                requested.from(), requested.to(), requested.granularity());
        // Use the complete trade universe so today's performance includes
        // trades that were fully closed today, matching the overview metric.
        boolean liveToday = isBeforeMarketClose();
        boolean replaceDailyPointWithLivePnl = requested.granularity() == PortfolioPerformanceGranularity.DAILY
                && liveToday;
        List<PortfolioPerformancePointDTO> points = result.points().stream().map(point -> PortfolioPerformancePointDTO.builder()
                .tradeDate(point.tradeDate()).marketValue(point.marketValue()).totalAssets(point.totalAssets())
                .costBasis(point.costBasis()).totalPnl(point.totalPnl())
                .periodPnl(point.tradeDate().equals(today) && replaceDailyPointWithLivePnl && todayPnl != null
                        ? todayPnl : point.periodPnl())
                .returnRate(point.returnRate()).build()).toList();
        boolean includesToday = !requested.from().isAfter(today) && !requested.to().isBefore(today);
        BigDecimal effectiveTodayPnl = !includesToday ? null : liveToday ? todayPnl : points.stream()
                .filter(point -> point.getTradeDate().equals(today))
                .map(PortfolioPerformancePointDTO::getPeriodPnl)
                .findFirst()
                .orElse(todayPnl);
        List<PortfolioPerformancePointDTO> summaryPoints = points;
        BigDecimal effectivePeriodPnl = summaryPoints.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal effectiveStartAssets = requested.from().equals(today) && todayPnl != null
                ? result.endAssets().subtract(todayPnl)
                : summaryPoints.isEmpty() ? result.startAssets()
                : summaryPoints.getFirst().getTotalAssets().subtract(summaryPoints.getFirst().getPeriodPnl());
        BigDecimal effectiveReturnRate = effectiveStartAssets.signum() > 0
                ? effectivePeriodPnl.divide(effectiveStartAssets, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal bestPeriodPnl = summaryPoints.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal worstPeriodPnl = summaryPoints.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        // The daily view visualizes the latest trading day's composition. Other
        // granularities visualize the full selected period's composition.
        Map<String, BigDecimal> effectiveContributions = new LinkedHashMap<>(
                requested.granularity() == PortfolioPerformanceGranularity.DAILY
                        ? result.lastDayPnlBySymbol()
                        : result.periodPnlBySymbol());
        boolean replacesTodayContribution = replaceDailyPointWithLivePnl && result.points().stream()
                .anyMatch(point -> point.tradeDate().equals(today));
        if (replacesTodayContribution) {
            if (requested.granularity() == PortfolioPerformanceGranularity.DAILY) {
                effectiveContributions.clear();
                effectiveContributions.putAll(todayPnlBySymbol);
            } else {
                result.lastDayPnlBySymbol().forEach((symbol, pnl) ->
                        effectiveContributions.merge(symbol, pnl.negate(), BigDecimal::add));
                todayPnlBySymbol.forEach((symbol, pnl) ->
                        effectiveContributions.merge(symbol, pnl, BigDecimal::add));
            }
        }
        Map<String, PortfolioTradeBO> contributionMetadata = trades.stream()
                .collect(Collectors.toMap(PortfolioTradeBO::symbol, trade -> trade,
                        (left, right) -> right, LinkedHashMap::new));
        List<PortfolioPerformanceContributionDTO> contributions = effectiveContributions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() != 0)
                .sorted((left, right) -> right.getValue().abs().compareTo(left.getValue().abs()))
                .map(entry -> {
                    PortfolioTradeBO metadata = contributionMetadata.get(entry.getKey());
                    return PortfolioPerformanceContributionDTO.builder()
                            .symbol(entry.getKey())
                            .securityName(metadata == null ? null : metadata.securityName())
                            .instrumentType(metadata == null ? null : metadata.instrumentType().name())
                            .periodPnl(entry.getValue())
                            .build();
                })
                .toList();
        return PortfolioPerformanceDTO.builder()
                .range(requested.range()).from(result.from()).to(result.to())
                .periodPnl(effectivePeriodPnl).todayPnl(effectiveTodayPnl).periodReturnRate(effectiveReturnRate)
                .startAssets(effectiveStartAssets).endAssets(result.endAssets())
                .bestPeriodPnl(bestPeriodPnl).worstPeriodPnl(worstPeriodPnl)
                .tradingDays(summaryPoints.size()).points(points)
                .contributions(contributions)
                .build();
    }

    private PerformanceRange resolveRange(String range, String fromValue, String toValue) {
        String normalized = range == null || range.isBlank() ? "daily" : range.trim().toLowerCase();
        LocalDate today = today();
        if (fromValue != null || toValue != null) {
            if (fromValue == null || fromValue.isBlank() || toValue == null || toValue.isBlank()) {
                throw new IllegalArgumentException("performance range requires both from and to");
            }
            LocalDate from = LocalDate.parse(fromValue);
            LocalDate to = LocalDate.parse(toValue);
            if (from.isAfter(to)) throw new IllegalArgumentException("performance from must not be after to");
            PortfolioPerformanceGranularity granularity = switch (normalized) {
                case "daily", "day", "custom" -> PortfolioPerformanceGranularity.DAILY;
                case "weekly", "week" -> PortfolioPerformanceGranularity.WEEKLY;
                case "monthly", "month" -> PortfolioPerformanceGranularity.MONTHLY;
                case "yearly", "year" -> PortfolioPerformanceGranularity.YEARLY;
                default -> throw new IllegalArgumentException("range must be daily, weekly, monthly, yearly, or custom");
            };
            to = min(to, today);
            if (from.isAfter(to)) throw new IllegalArgumentException("performance range is after today");
            if (normalized.equals("custom") && from.isBefore(today.minusDays(520))) {
                throw new IllegalArgumentException("custom performance range is limited to the latest 520 days");
            }
            return new PerformanceRange(normalized, from, to, granularity);
        }
        return switch (normalized) {
            case "daily", "day" -> new PerformanceRange("daily", today.minusDays(29), today,
                    PortfolioPerformanceGranularity.DAILY);
            case "weekly", "week" -> new PerformanceRange("weekly", today.minusWeeks(11).with(java.time.DayOfWeek.MONDAY), today,
                    PortfolioPerformanceGranularity.WEEKLY);
            case "monthly", "month" -> new PerformanceRange("monthly", today.withDayOfMonth(1).minusMonths(11), today,
                    PortfolioPerformanceGranularity.MONTHLY);
            case "yearly", "year" -> new PerformanceRange("yearly", today.withDayOfYear(1).minusYears(4), today,
                    PortfolioPerformanceGranularity.YEARLY);
            case "custom" -> {
                if (fromValue == null || fromValue.isBlank() || toValue == null || toValue.isBlank()) {
                    throw new IllegalArgumentException("custom performance range requires from and to");
                }
                LocalDate from = LocalDate.parse(fromValue);
                LocalDate to = LocalDate.parse(toValue);
                if (from.isAfter(to)) throw new IllegalArgumentException("performance from must not be after to");
                to = min(to, today);
                if (from.isAfter(to)) throw new IllegalArgumentException("performance range is after today");
                if (from.isBefore(today.minusDays(520))) {
                    throw new IllegalArgumentException("custom performance range is limited to the latest 520 days");
                }
                yield new PerformanceRange("custom", from, to.isAfter(today) ? today : to,
                        PortfolioPerformanceGranularity.DAILY);
            }
            default -> throw new IllegalArgumentException("range must be daily, weekly, monthly, yearly, or custom");
        };
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private record PerformanceRange(String range, LocalDate from, LocalDate to,
                                    PortfolioPerformanceGranularity granularity) {
    }

    private PortfolioPositionDTO toPosition(cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO value,
                                             Map<String, String> securityNames,
                                             Map<String, BigDecimal> todayPnlBySymbol) {
        return PortfolioPositionDTO.builder().symbol(value.symbol()).securityName(securityNames.get(value.symbol()))
                .instrumentType(value.instrumentType().name()).quantity(value.quantity())
                .averageCost(value.averageCost()).costBasis(value.costBasis()).realizedPnl(value.realizedPnl())
                .latestPrice(value.latestPrice()).marketValue(value.marketValue()).unrealizedPnl(value.unrealizedPnl())
                .todayPnl(todayPnlBySymbol.get(value.symbol()))
                .unrealizedReturnRate(value.unrealizedReturnRate()).build();
    }

    private Map<String, List<PortfolioPricePointBO>> loadPortfolioPriceSeries(List<PortfolioTradeBO> trades) {
        Map<String, PortfolioInstrumentType> instruments = new LinkedHashMap<>();
        for (PortfolioTradeBO trade : trades) {
            instruments.putIfAbsent(trade.symbol(), trade.instrumentType());
        }
        Map<String, List<PortfolioPricePointBO>> prices = new LinkedHashMap<>();
        for (Map.Entry<String, PortfolioInstrumentType> entry : instruments.entrySet()) {
            try {
                var result = stockChartQueryDomainService.klines(entry.getValue().chartTargetType(), entry.getKey(), "1d", "qfq");
                prices.put(entry.getKey(), result.items().stream()
                        .filter(bar -> bar.getTradeDate() != null && bar.getClose() != null)
                        .map(bar -> new PortfolioPricePointBO(bar.getTradeDate(), bar.getClose()))
                        .toList());
            } catch (RuntimeException exception) {
                log.warn("Unable to load portfolio daily prices for symbol={}", entry.getKey(), exception);
                prices.put(entry.getKey(), List.of());
            }
        }
        return prices;
    }

    private List<PortfolioPricePointBO> appendPricePoint(List<PortfolioPricePointBO> series,
                                                         LocalDate tradeDate,
                                                         BigDecimal close) {
        List<PortfolioPricePointBO> updated = new java.util.ArrayList<>(series);
        updated.add(new PortfolioPricePointBO(tradeDate, close));
        return updated;
    }

    private Map<String, List<PortfolioIntradayPricePointBO>> loadPortfolioIntradayPriceSeries(
            List<PortfolioTradeBO> trades, LocalDate today) {
        if (!usePostCloseMinuteData()) {
            return Map.of();
        }
        Map<String, PortfolioInstrumentType> instruments = new LinkedHashMap<>();
        for (PortfolioTradeBO trade : trades) {
            if (trade != null) {
                instruments.putIfAbsent(trade.symbol(), trade.instrumentType());
            }
        }
        Map<String, List<PortfolioIntradayPricePointBO>> prices = new LinkedHashMap<>();
        for (Map.Entry<String, PortfolioInstrumentType> entry : instruments.entrySet()) {
            try {
                var result = stockChartQueryDomainService.intraday(
                        entry.getValue().chartTargetType(), entry.getKey(), entry.getKey(), "qfq",
                        today.toString(), List.of("1m"));
                if (result == null || !today.equals(result.tradeDate())) {
                    continue;
                }
                List<PortfolioIntradayPricePointBO> points = result.timeshare().stream()
                        .filter(point -> point.getTimestamp() != null && point.getPrice() != null)
                        .map(point -> new PortfolioIntradayPricePointBO(
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(point.getTimestamp()), SHANGHAI),
                                point.getPrice()))
                        .toList();
                if (points.isEmpty()) {
                    points = result.minuteBars().getOrDefault("1m", List.of()).stream()
                            .filter(point -> point.getTimestamp() != null && point.getClose() != null)
                            .map(point -> new PortfolioIntradayPricePointBO(
                                    LocalDateTime.ofInstant(Instant.ofEpochMilli(point.getTimestamp()), SHANGHAI),
                                    point.getClose()))
                            .toList();
                }
                if (!points.isEmpty()) {
                    prices.put(entry.getKey(), points);
                }
            } catch (RuntimeException exception) {
                log.warn("Unable to load portfolio intraday prices for symbol={} date={}", entry.getKey(), today, exception);
            }
        }
        return prices;
    }

    private Map<String, BigDecimal> latestIntradayPrices(
            Map<String, List<PortfolioIntradayPricePointBO>> intradayPriceSeries) {
        if (intradayPriceSeries == null || intradayPriceSeries.isEmpty()) return Map.of();
        Map<String, BigDecimal> latest = new HashMap<>();
        intradayPriceSeries.forEach((symbol, points) -> points.stream()
                .max(java.util.Comparator.comparing(PortfolioIntradayPricePointBO::tradedAt))
                .ifPresent(point -> latest.put(symbol, point.price())));
        return latest;
    }

    private List<PortfolioPositionBO> quotePositions(List<PortfolioTradeBO> trades,
                                                      List<PortfolioPositionBO> valuedPositions) {
        Map<String, PortfolioPositionBO> positions = new LinkedHashMap<>();
        if (valuedPositions != null) {
            valuedPositions.stream().filter(java.util.Objects::nonNull)
                    .forEach(position -> positions.put(position.symbol(), position));
        }
        if (trades != null) {
            trades.stream().filter(java.util.Objects::nonNull)
                    .filter(trade -> trade.status() == cn.cccs7.cynexus.market.domain.portfolio.TradeStatus.ACTIVE)
                    .forEach(trade -> positions.putIfAbsent(trade.symbol(), new PortfolioPositionBO(
                            trade.symbol(), trade.instrumentType(), BigDecimal.ONE, null, null, null,
                            null, null, null, null)));
        }
        return List.copyOf(positions.values());
    }

    private boolean usePostCloseMinuteData() {
        return !java.time.LocalDateTime.now(clock).toLocalTime().isBefore(MARKET_CLOSE);
    }

    private boolean isBeforeMarketClose() {
        return !usePostCloseMinuteData();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Map<String, String> loadSecurityNames(List<PortfolioPositionBO> positions) {
        Map<String, String> names = new HashMap<>();
        for (PortfolioPositionBO position : positions) {
            try {
                var meta = stockChartQueryDomainService.stockMeta(position.symbol());
                if (meta != null && meta.getName() != null && !meta.getName().isBlank()
                        && !meta.getName().equals(position.symbol())) {
                    names.put(position.symbol(), meta.getName());
                }
            } catch (RuntimeException exception) {
                log.warn("Unable to load portfolio security name for symbol={}", position.symbol(), exception);
            }
        }
        return names;
    }

    private Map<String, BigDecimal> loadLatestPrices(List<PortfolioPositionBO> positions) {
        Map<String, BigDecimal> latestPrices = new HashMap<>(realtimePriceGateway.latestPrices(positions));
        for (PortfolioPositionBO position : positions) {
            if (position.quantity() == null || position.quantity().signum() <= 0) {
                continue;
            }
            if (latestPrices.containsKey(position.symbol())) {
                continue;
            }
            try {
                var result = stockChartQueryDomainService.klines(position.instrumentType().chartTargetType(), position.symbol(), "1d", "qfq");
                result.items().stream()
                        .filter(bar -> bar.getClose() != null)
                        .max((left, right) -> {
                            if (left.getTradeDate() == null && right.getTradeDate() == null) return 0;
                            if (left.getTradeDate() == null) return -1;
                            if (right.getTradeDate() == null) return 1;
                            return left.getTradeDate().compareTo(right.getTradeDate());
                        })
                        .ifPresent(bar -> latestPrices.put(position.symbol(), bar.getClose()));
            } catch (RuntimeException exception) {
                log.warn("Unable to load latest portfolio price for symbol={}", position.symbol(), exception);
            }
        }
        return latestPrices;
    }

    private BigDecimal currentMarketValue(List<PortfolioTradeBO> trades) {
        var positions = ledgerService.calculate(trades, Map.of()).positions();
        var valued = ledgerService.calculate(trades, loadLatestPrices(positions));
        return sum(valued.positions(), PortfolioPositionBO::marketValue);
    }

    private BigDecimal accumulatedPnlSinceAccountBaseline(
            PortfolioAccountBO account,
            List<PortfolioTradeBO> trades,
            Map<String, List<PortfolioPricePointBO>> priceSeries,
            Map<String, BigDecimal> baselines,
            LocalDate today,
            BigDecimal todayPnl) {
        if (account == null || account.totalAssets() == null || account.updatedAt() == null || today == null) {
            return BigDecimal.ZERO;
        }
        LocalDate baselineDate = account.updatedAt().atZoneSameInstant(SHANGHAI).toLocalDate();
        if (!baselineDate.isBefore(today)) {
            return BigDecimal.ZERO;
        }
        BigDecimal historicalPnl = performanceDomainService.cumulativePnl(
                trades, priceSeries, baselines, baselineDate.plusDays(1), today.minusDays(1));
        return historicalPnl.add(todayPnl == null ? BigDecimal.ZERO : todayPnl);
    }

    private BigDecimal sum(List<PortfolioPositionBO> positions,
                           java.util.function.Function<PortfolioPositionBO, BigDecimal> value) {
        return positions.stream().map(value).filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
