package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPositionDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioLedgerDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PortfolioOverviewAppService {
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private final PortfolioTradeQueryRepository tradeRepository;
    private final PortfolioLedgerDomainService ledgerService;

    public PortfolioOverviewAppService(PortfolioTradeQueryRepository tradeRepository,
                                       PortfolioLedgerDomainService ledgerService) {
        this.tradeRepository = tradeRepository;
        this.ledgerService = ledgerService;
    }

    public PortfolioOverviewDTO overview() {
        var trades = tradeRepository.list(PRINCIPAL, TENANT, null);
        var result = ledgerService.calculate(trades, Map.of());
        return PortfolioOverviewDTO.builder()
                .positions(result.positions().stream().map(this::toPosition).toList())
                .realizedPnl(result.realizedPnl()).unrealizedPnl(result.unrealizedPnl()).totalPnl(result.totalPnl())
                .tradeCount(trades.size()).valuationStatus("LATEST_PRICE_UNAVAILABLE").build();
    }

    public List<PortfolioTradeDTO> trades(String symbol) {
        return tradeRepository.list(PRINCIPAL, TENANT, symbol).stream().map(trade -> PortfolioTradeDTO.builder()
                .id(trade.id()).symbol(trade.symbol()).securityName(trade.securityName()).side(trade.side().name())
                .tradedAt(trade.tradedAt()).quantity(trade.quantity()).price(trade.price())
                .totalFee(trade.totalFee()).status(trade.status().name()).build()).toList();
    }

    private PortfolioPositionDTO toPosition(cn.cccs7.cynexus.market.domain.portfolio.PortfolioPositionBO value) {
        return PortfolioPositionDTO.builder().symbol(value.symbol()).quantity(value.quantity())
                .averageCost(value.averageCost()).costBasis(value.costBasis()).realizedPnl(value.realizedPnl())
                .latestPrice(value.latestPrice()).marketValue(value.marketValue()).unrealizedPnl(value.unrealizedPnl())
                .unrealizedReturnRate(value.unrealizedReturnRate()).build();
    }
}
