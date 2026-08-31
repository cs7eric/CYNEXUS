package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioTradeUpdateRequest;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeUpdateDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioTradeAppService {
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private final PortfolioTradeUpdateDomainService updateDomainService;

    public PortfolioTradeAppService(PortfolioTradeUpdateDomainService updateDomainService) {
        this.updateDomainService = updateDomainService;
    }

    @Transactional
    public PortfolioTradeDTO update(long tradeId, PortfolioTradeUpdateRequest request) {
        if (request == null) throw new IllegalArgumentException("portfolio trade update is required");
        PortfolioTradeBO trade = updateDomainService.update(PRINCIPAL, TENANT, tradeId,
                request.getSymbol(), request.getSecurityName(), request.getInstrumentType(), request.getSide(),
                request.getTradedAt(), request.getQuantity(), request.getPrice(), request.getTotalFee(), request.getStrategy(), request.getTags());
        return toDto(trade);
    }

    private PortfolioTradeDTO toDto(PortfolioTradeBO trade) {
        return PortfolioTradeDTO.builder().id(trade.id()).symbol(trade.symbol()).securityName(trade.securityName())
                .instrumentType(trade.instrumentType().name()).side(trade.side().name()).tradedAt(trade.tradedAt())
                .quantity(trade.quantity()).price(trade.price()).totalFee(trade.totalFee()).status(trade.status().name()).strategy(trade.strategy()).tags(trade.tags())
                .industryCode(trade.industryCode()).industryName(trade.industryName())
                .build();
    }
}
