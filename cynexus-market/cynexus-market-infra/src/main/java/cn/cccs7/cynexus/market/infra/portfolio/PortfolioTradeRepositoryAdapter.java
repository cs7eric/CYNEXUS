package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioTradeMapper;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioTradeRepositoryAdapter implements PortfolioTradeRepository {
    private final PortfolioTradeMapper mapper;
    private final PortfolioIdGenerator idGenerator;

    public PortfolioTradeRepositoryAdapter(PortfolioTradeMapper mapper, PortfolioIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public boolean save(String principalKey, String tenantKey, long sourceTaskId, long sourceCandidateId,
                        PortfolioTradeBO trade, String confirmedBy) {
        PortfolioTradePO po = new PortfolioTradePO();
        po.setId(idGenerator.nextId());
        po.setPrincipalKey(principalKey);
        po.setTenantKey(tenantKey);
        po.setSourceTaskId(sourceTaskId);
        po.setSourceCandidateId(sourceCandidateId);
        po.setSymbol(trade.symbol());
        po.setSecurityName(trade.securityName());
        po.setInstrumentType(trade.instrumentType().name());
        po.setSide(trade.side().name());
        po.setTradedAt(trade.tradedAt());
        po.setQuantity(trade.quantity());
        po.setPrice(trade.price());
        po.setGrossAmount(trade.quantity().multiply(trade.price()));
        po.setTotalFee(trade.totalFee());
        po.setStatus(trade.status().name());
        po.setConfirmedAt(OffsetDateTime.now());
        po.setConfirmedBy(confirmedBy);
        return mapper.insert(po) == 1;
    }
}
