package cn.cccs7.cynexus.market.domain.portfolio;

import org.springframework.stereotype.Service;

@Service
public class PortfolioTradeUpdateDomainService {
    private final PortfolioTradeQueryRepository queryRepository;
    private final PortfolioTradeRepository tradeRepository;

    public PortfolioTradeUpdateDomainService(PortfolioTradeQueryRepository queryRepository,
                                             PortfolioTradeRepository tradeRepository) {
        this.queryRepository = queryRepository;
        this.tradeRepository = tradeRepository;
    }

    public PortfolioTradeBO update(String principalKey, String tenantKey, long tradeId,
                                   String symbol, String securityName, String instrumentType, String side,
                                   String tradedAt, String quantity, String price, String totalFee) {
        PortfolioTradeBO current = queryRepository.findById(principalKey, tenantKey, tradeId)
                .orElseThrow(() -> new IllegalArgumentException("portfolio trade not found"));
        if (current.status() != TradeStatus.ACTIVE) {
            throw new IllegalStateException("only active portfolio trades can be updated");
        }
        PortfolioTradeBO updated = PortfolioTradeEditPolicy.normalize(current.id(), symbol, securityName,
                instrumentType, side, tradedAt, quantity, price, totalFee, current.status());
        if (!tradeRepository.update(principalKey, tenantKey, updated)) {
            throw new IllegalStateException("portfolio trade was changed concurrently: " + tradeId);
        }
        return updated;
    }
}
