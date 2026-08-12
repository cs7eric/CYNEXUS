package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewAnchorBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewAnchorRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioReviewAnchorPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioReviewAnchorMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioReviewAnchorRepositoryAdapter implements PortfolioReviewAnchorRepository {
    private final PortfolioReviewAnchorMapper mapper;

    public PortfolioReviewAnchorRepositoryAdapter(PortfolioReviewAnchorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PortfolioReviewAnchorBO> list(String principalKey, String tenantKey, String symbol, LocalDate tradeDate) {
        return mapper.list(principalKey, tenantKey, symbol, tradeDate).stream().map(this::toBo).toList();
    }

    @Override
    public long create(PortfolioReviewAnchorBO anchor) {
        mapper.insert(toPo(anchor));
        return anchor.id();
    }

    @Override
    public boolean update(PortfolioReviewAnchorBO anchor) {
        return mapper.update(toPo(anchor)) == 1;
    }

    @Override
    public boolean delete(String principalKey, String tenantKey, long id) {
        return mapper.softDelete(principalKey, tenantKey, id) == 1;
    }

    private PortfolioReviewAnchorPO toPo(PortfolioReviewAnchorBO value) {
        PortfolioReviewAnchorPO po = new PortfolioReviewAnchorPO();
        po.setId(value.id());
        po.setPrincipalKey(value.principalKey());
        po.setTenantKey(value.tenantKey());
        po.setSymbol(value.symbol());
        po.setInstrumentType(value.instrumentType());
        po.setTradeId(value.tradeId());
        po.setReviewId(value.reviewId());
        po.setAnchorType(value.anchorType());
        po.setSide(value.side());
        po.setOccurredAt(value.occurredAt());
        po.setPrice(value.price());
        po.setNote(value.note());
        po.setTags(value.tags());
        return po;
    }

    private PortfolioReviewAnchorBO toBo(PortfolioReviewAnchorPO value) {
        return new PortfolioReviewAnchorBO(value.getId(), value.getPrincipalKey(), value.getTenantKey(), value.getSymbol(),
                value.getInstrumentType(), value.getTradeId(), value.getReviewId(), value.getAnchorType(), value.getSide(),
                value.getOccurredAt(), value.getPrice(), value.getNote(), value.getTags());
    }
}
