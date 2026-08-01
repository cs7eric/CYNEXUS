package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeQueryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.TradeSide;
import cn.cccs7.cynexus.market.domain.portfolio.TradeStatus;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioTradeQueryMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioTradeQueryRepositoryAdapter implements PortfolioTradeQueryRepository {
    private final PortfolioTradeQueryMapper mapper;

    public PortfolioTradeQueryRepositoryAdapter(PortfolioTradeQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PortfolioTradeBO> list(String principalKey, String tenantKey, String symbol) {
        return mapper.list(principalKey, tenantKey, symbol).stream().map(this::toDomain).toList();
    }

    private PortfolioTradeBO toDomain(PortfolioTradePO po) {
        return new PortfolioTradeBO(po.getId(), po.getSymbol(), po.getSecurityName(), TradeSide.valueOf(po.getSide()),
                po.getTradedAt(), po.getQuantity(), po.getPrice(), po.getTotalFee(), TradeStatus.valueOf(po.getStatus()));
    }
}
