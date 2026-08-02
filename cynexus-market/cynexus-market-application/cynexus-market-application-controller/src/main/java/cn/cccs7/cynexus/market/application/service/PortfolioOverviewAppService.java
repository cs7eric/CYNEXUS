package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPositionDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerResultBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioOverviewAppService {
    private static final Logger log = LoggerFactory.getLogger(PortfolioOverviewAppService.class);
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private final PortfolioTradeQueryRepository tradeRepository;
    private final PortfolioLedgerDomainService ledgerService;
    private final StockChartQueryDomainService stockChartQueryDomainService;

    public PortfolioOverviewAppService(PortfolioTradeQueryRepository tradeRepository,
                                       PortfolioLedgerDomainService ledgerService,
                                       StockChartQueryDomainService stockChartQueryDomainService) {
        this.tradeRepository = tradeRepository;
        this.ledgerService = ledgerService;
        this.stockChartQueryDomainService = stockChartQueryDomainService;
    }

    public PortfolioOverviewDTO overview() {
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        var preliminary = ledgerService.calculate(trades, Map.of());
        Map<String, BigDecimal> latestPrices = loadLatestPrices(preliminary.positions());
        PortfolioLedgerResultBO result = ledgerService.calculate(trades, latestPrices);
        BigDecimal costBasis = sum(result.positions(), PortfolioPositionBO::costBasis);
        BigDecimal marketValue = sum(result.positions(), PortfolioPositionBO::marketValue);
        return PortfolioOverviewDTO.builder()
                .positions(result.positions().stream().map(this::toPosition).toList())
                .costBasis(costBasis).marketValue(marketValue)
                .realizedPnl(result.realizedPnl()).unrealizedPnl(result.unrealizedPnl()).totalPnl(result.totalPnl())
                .tradeCount(trades.size())
                .valuationStatus(ledgerService.valuationStatus(trades, result, latestPrices).name()).build();
    }

    public List<PortfolioTradeDTO> trades(String symbol) {
        return tradeRepository.list(PRINCIPAL, TENANT, symbol).stream().map(trade -> PortfolioTradeDTO.builder()
                .id(trade.id()).symbol(trade.symbol()).securityName(trade.securityName()).instrumentType(trade.instrumentType().name()).side(trade.side().name())
                .tradedAt(trade.tradedAt()).quantity(trade.quantity()).price(trade.price())
                .totalFee(trade.totalFee()).status(trade.status().name()).build()).toList();
    }

    private PortfolioPositionDTO toPosition(cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO value) {
        return PortfolioPositionDTO.builder().symbol(value.symbol()).instrumentType(value.instrumentType().name()).quantity(value.quantity())
                .averageCost(value.averageCost()).costBasis(value.costBasis()).realizedPnl(value.realizedPnl())
                .latestPrice(value.latestPrice()).marketValue(value.marketValue()).unrealizedPnl(value.unrealizedPnl())
                .unrealizedReturnRate(value.unrealizedReturnRate()).build();
    }

    private Map<String, BigDecimal> loadLatestPrices(List<PortfolioPositionBO> positions) {
        Map<String, BigDecimal> latestPrices = new HashMap<>();
        for (PortfolioPositionBO position : positions) {
            if (position.quantity() == null || position.quantity().signum() <= 0) {
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
