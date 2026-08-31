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
        return update(principalKey, tenantKey, tradeId, symbol, securityName, instrumentType, side, tradedAt, quantity, price, totalFee, null, null);
    }

    public PortfolioTradeBO update(String principalKey, String tenantKey, long tradeId,
                                   String symbol, String securityName, String instrumentType, String side,
                                   String tradedAt, String quantity, String price, String totalFee, String strategy, String tags) {
        PortfolioTradeBO current = queryRepository.findById(principalKey, tenantKey, tradeId)
                .orElseThrow(() -> new IllegalArgumentException("portfolio trade not found"));
        if (current.status() != TradeStatus.ACTIVE) {
            throw new IllegalStateException("only active portfolio trades can be updated");
        }
        PortfolioTradeBO updated = PortfolioTradeEditPolicy.normalize(current.id(), symbol, securityName,
                instrumentType, side, tradedAt, quantity, price, totalFee, current.status());
        updated = new PortfolioTradeBO(updated.id(), updated.symbol(), updated.securityName(), updated.instrumentType(), updated.side(), updated.tradedAt(), updated.quantity(), updated.price(), updated.totalFee(), updated.status(), strategy == null ? current.strategy() : strategy, tags == null ? current.tags() : tags, current.industryCode(), current.industryName());
        if (!tradeRepository.update(principalKey, tenantKey, updated)) {
            throw new IllegalStateException("portfolio trade was changed concurrently: " + tradeId);
        }
        return updated;
    }
}
