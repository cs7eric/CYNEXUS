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
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformancePointDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealizedPnlBaselineRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioInstrumentType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceGranularity;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPerformanceResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPricePointBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealtimePriceGateway;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioOverviewAppService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioOverviewAppService.class);
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final PortfolioTradeQueryRepository tradeRepository;
    private final PortfolioRealizedPnlBaselineRepository realizedPnlBaselineRepository;
    private final PortfolioAccountDomainService accountDomainService;
    private final PortfolioLedgerDomainService ledgerService;
    private final StockChartQueryDomainService stockChartQueryDomainService;
    private final PortfolioRealtimePriceGateway realtimePriceGateway;
    private final PortfolioPerformanceDomainService performanceDomainService;

    public PortfolioOverviewAppService(PortfolioTradeQueryRepository tradeRepository,
                                       PortfolioRealizedPnlBaselineRepository realizedPnlBaselineRepository,
                                       PortfolioAccountDomainService accountDomainService,
                                       PortfolioLedgerDomainService ledgerService,
                                       StockChartQueryDomainService stockChartQueryDomainService,
                                       PortfolioRealtimePriceGateway realtimePriceGateway,
                                       PortfolioPerformanceDomainService performanceDomainService) {
        this.tradeRepository = tradeRepository;
        this.realizedPnlBaselineRepository = realizedPnlBaselineRepository;
        this.accountDomainService = accountDomainService;
        this.ledgerService = ledgerService;
        this.stockChartQueryDomainService = stockChartQueryDomainService;
        this.realtimePriceGateway = realtimePriceGateway;
        this.performanceDomainService = performanceDomainService;
    }

    public PortfolioOverviewDTO overview() {
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        var baselines = realizedPnlBaselineRepository.list(PRINCIPAL, TENANT);
        var preliminary = ledgerService.applyRealizedPnlBaselines(
                ledgerService.calculate(trades, Map.of()), baselines);
        Map<String, BigDecimal> latestPrices = loadLatestPrices(preliminary.positions());
        Map<String, BigDecimal> previousClosePrices = realtimePriceGateway.previousClosePrices(preliminary.positions());
        Map<String, LocalDate> realtimePriceDates = realtimePriceGateway.latestPriceDates(preliminary.positions());
        PortfolioLedgerResultBO result = ledgerService.applyRealizedPnlBaselines(
                ledgerService.calculate(trades, latestPrices), baselines);
        Map<String, List<PortfolioPricePointBO>> priceSeries = loadPortfolioPriceSeries(preliminary.positions());
        Map<String, BigDecimal> todayPnlBySymbol = performanceDomainService.calculateTodayPnlBySymbol(
                trades, priceSeries, latestPrices, previousClosePrices, realtimePriceDates, LocalDate.now(SHANGHAI));
        BigDecimal costBasis = sum(result.positions(), PortfolioPositionBO::costBasis);
        BigDecimal marketValue = sum(result.positions(), PortfolioPositionBO::marketValue);
        PortfolioAccountBO account = accountDomainService.get(PRINCIPAL, TENANT);
        BigDecimal availableCash = account.availableCash();
        Map<String, String> securityNames = loadSecurityNames(result.positions());
        return PortfolioOverviewDTO.builder()
                .positions(result.positions().stream().map(position -> toPosition(position, securityNames, todayPnlBySymbol)).toList())
                .dataQualityWarnings(result.warnings().stream().map(warning -> PortfolioLedgerWarningDTO.builder()
                        .code(warning.code()).symbol(warning.symbol())
                        .unmatchedQuantity(warning.unmatchedQuantity()).message(warning.message()).build()).toList())
                .costBasis(costBasis).marketValue(marketValue)
                .availableCash(availableCash).totalAssets(marketValue.add(availableCash))
                .accountUpdatedAt(account.updatedAt())
                .realizedPnl(result.realizedPnl()).unrealizedPnl(result.unrealizedPnl()).totalPnl(result.totalPnl())
                .todayPnl(todayPnlBySymbol.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .tradeCount(trades.size())
                .valuationStatus(ledgerService.valuationStatus(trades, result, latestPrices).name()).build();
    }

    public PortfolioAccountDTO account() {
        PortfolioAccountBO account = accountDomainService.get(PRINCIPAL, TENANT);
        return PortfolioAccountDTO.builder().availableCash(account.availableCash()).updatedAt(account.updatedAt()).build();
    }

    public PortfolioAccountDTO updateAccount(PortfolioAccountUpdateRequest request) {
        if (request == null) throw new IllegalArgumentException("portfolio account update is required");
        PortfolioAccountBO account = accountDomainService.updateAvailableCash(PRINCIPAL, TENANT, request.getAvailableCash());
        return PortfolioAccountDTO.builder().availableCash(account.availableCash()).updatedAt(account.updatedAt()).build();
    }

    public List<PortfolioTradeDTO> trades(String symbol) {
        return tradeRepository.list(PRINCIPAL, TENANT, symbol).stream().map(trade -> PortfolioTradeDTO.builder()
                .id(trade.id()).symbol(trade.symbol()).securityName(trade.securityName()).instrumentType(trade.instrumentType().name()).side(trade.side().name())
                .tradedAt(trade.tradedAt()).quantity(trade.quantity()).price(trade.price())
                .totalFee(trade.totalFee()).status(trade.status().name()).build()).toList();
    }

    public PortfolioPerformanceDTO performance(String range, String fromValue, String toValue) {
        LocalDate today = LocalDate.now(SHANGHAI);
        OffsetDateTime latestResetAt = realizedPnlBaselineRepository.latestResetAt(PRINCIPAL, TENANT);
        LocalDate trackingStartDate = latestResetAt == null ? today : latestResetAt.atZoneSameInstant(SHANGHAI).toLocalDate();
        PerformanceRange requested = resolveRange(range, fromValue, toValue, trackingStartDate);
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        var openPositions = ledgerService.calculate(trades, Map.of()).positions();
        Map<String, BigDecimal> realtimePrices = realtimePriceGateway.latestPrices(openPositions);
        Map<String, BigDecimal> previousClosePrices = realtimePriceGateway.previousClosePrices(openPositions);
        Map<String, LocalDate> realtimePriceDates = realtimePriceGateway.latestPriceDates(openPositions);
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
                if (realtimePrice != null && hasFreshRealtimePrice) {
                    series = series.stream().filter(point -> !point.tradeDate().equals(today)).collect(Collectors.toCollection(java.util.ArrayList::new));
                    series.add(new PortfolioPricePointBO(today, realtimePrice));
                } else if (series.stream().noneMatch(point -> point.tradeDate().equals(today))) {
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
        PortfolioPerformanceResultBO result = performanceDomainService.calculate(
                trades, prices, realizedPnlBaselineRepository.list(PRINCIPAL, TENANT), account.availableCash(),
                requested.from(), requested.to(), requested.granularity());
        BigDecimal todayPnl = performanceDomainService.calculateTodayPnl(trades, prices, realtimePrices, previousClosePrices, realtimePriceDates, today);
        List<PortfolioPerformancePointDTO> points = result.points().stream().map(point -> PortfolioPerformancePointDTO.builder()
                .tradeDate(point.tradeDate()).marketValue(point.marketValue()).totalAssets(point.totalAssets())
                .costBasis(point.costBasis()).totalPnl(point.totalPnl())
                .periodPnl(point.tradeDate().equals(today) && todayPnl != null ? todayPnl : point.periodPnl())
                .returnRate(point.returnRate()).build()).toList();
        BigDecimal effectivePeriodPnl = points.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal effectiveStartAssets = trackingStartDate.equals(today) && todayPnl != null
                ? result.endAssets().subtract(todayPnl) : result.startAssets();
        BigDecimal effectiveReturnRate = effectiveStartAssets.signum() > 0
                ? effectivePeriodPnl.divide(effectiveStartAssets, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal bestPeriodPnl = points.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal worstPeriodPnl = points.stream().map(PortfolioPerformancePointDTO::getPeriodPnl)
                .filter(java.util.Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return PortfolioPerformanceDTO.builder()
                .range(requested.range()).from(result.from()).to(result.to())
                .periodPnl(effectivePeriodPnl).todayPnl(todayPnl).periodReturnRate(effectiveReturnRate)
                .startAssets(effectiveStartAssets).endAssets(result.endAssets())
                .bestPeriodPnl(bestPeriodPnl).worstPeriodPnl(worstPeriodPnl)
                .tradingDays(points.size()).points(points)
                .build();
    }

    private PerformanceRange resolveRange(String range, String fromValue, String toValue, LocalDate trackingStartDate) {
        String normalized = range == null || range.isBlank() ? "daily" : range.trim().toLowerCase();
        LocalDate today = LocalDate.now(SHANGHAI);
        return switch (normalized) {
            case "daily", "day" -> new PerformanceRange("daily", max(today.minusDays(29), trackingStartDate), today,
                    PortfolioPerformanceGranularity.DAILY);
            case "weekly", "week" -> new PerformanceRange("weekly", max(today.minusWeeks(11).with(java.time.DayOfWeek.MONDAY), trackingStartDate), today,
                    PortfolioPerformanceGranularity.WEEKLY);
            case "monthly", "month" -> new PerformanceRange("monthly", max(today.withDayOfMonth(1).minusMonths(11), trackingStartDate), today,
                    PortfolioPerformanceGranularity.MONTHLY);
            case "yearly", "year" -> new PerformanceRange("yearly", max(today.withDayOfYear(1).minusYears(4), trackingStartDate), today,
                    PortfolioPerformanceGranularity.YEARLY);
            case "custom" -> {
                if (fromValue == null || fromValue.isBlank() || toValue == null || toValue.isBlank()) {
                    throw new IllegalArgumentException("custom performance range requires from and to");
                }
                LocalDate from = LocalDate.parse(fromValue);
                LocalDate to = LocalDate.parse(toValue);
                if (from.isAfter(to)) throw new IllegalArgumentException("performance from must not be after to");
                from = max(from, trackingStartDate);
                to = min(to, today);
                if (from.isAfter(to)) throw new IllegalArgumentException("performance range is before the tracking start date");
                if (from.isBefore(today.minusDays(520))) {
                    throw new IllegalArgumentException("custom performance range is limited to the latest 520 days");
                }
                yield new PerformanceRange("custom", from, to.isAfter(today) ? today : to,
                        PortfolioPerformanceGranularity.DAILY);
            }
            default -> throw new IllegalArgumentException("range must be daily, weekly, monthly, yearly, or custom");
        };
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
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

    private Map<String, List<PortfolioPricePointBO>> loadPortfolioPriceSeries(List<PortfolioPositionBO> positions) {
        Map<String, List<PortfolioPricePointBO>> prices = new LinkedHashMap<>();
        for (PortfolioPositionBO position : positions) {
            try {
                var result = stockChartQueryDomainService.klines(position.instrumentType().chartTargetType(), position.symbol(), "1d", "qfq");
                prices.put(position.symbol(), result.items().stream()
                        .filter(bar -> bar.getTradeDate() != null && bar.getClose() != null)
                        .map(bar -> new PortfolioPricePointBO(bar.getTradeDate(), bar.getClose()))
                        .toList());
            } catch (RuntimeException exception) {
                log.warn("Unable to load portfolio daily prices for symbol={}", position.symbol(), exception);
                prices.put(position.symbol(), List.of());
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

    private BigDecimal sum(List<PortfolioPositionBO> positions,
                           java.util.function.Function<PortfolioPositionBO, BigDecimal> value) {
        return positions.stream().map(value).filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
