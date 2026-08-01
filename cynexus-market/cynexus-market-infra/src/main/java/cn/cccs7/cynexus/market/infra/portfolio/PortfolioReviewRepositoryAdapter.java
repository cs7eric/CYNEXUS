package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioReviewRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioReviewPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioReviewMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioReviewRepositoryAdapter implements PortfolioReviewRepository {
    private final PortfolioReviewMapper mapper;
    public PortfolioReviewRepositoryAdapter(PortfolioReviewMapper mapper) { this.mapper = mapper; }
    @Override public long create(PortfolioReviewBO review) { mapper.insert(toPo(review)); return review.id(); }
    @Override public boolean update(PortfolioReviewBO review) { return mapper.update(toPo(review)) == 1; }
    @Override public Optional<PortfolioReviewBO> findById(String p, String t, long id) { return Optional.ofNullable(mapper.findById(p, t, id)).map(this::toBo); }
    @Override public List<PortfolioReviewBO> list(String p, String t, String symbol) { return mapper.list(p, t, symbol).stream().map(this::toBo).toList(); }
    private PortfolioReviewPO toPo(PortfolioReviewBO value) { PortfolioReviewPO p = new PortfolioReviewPO(); p.setId(value.id()); p.setPrincipalKey(value.principalKey()); p.setTenantKey(value.tenantKey()); p.setSymbol(value.symbol()); p.setPeriodStart(value.periodStart()); p.setPeriodEnd(value.periodEnd()); p.setThesis(value.thesis()); p.setPlan(value.plan()); p.setExecution(value.execution()); p.setLessons(value.lessons()); p.setNextAction(value.nextAction()); p.setTags(value.tags()); p.setRating(value.rating()); p.setMetricsSnapshot(value.metricsSnapshot()); p.setStatus(value.status()); return p; }
    private PortfolioReviewBO toBo(PortfolioReviewPO p) { return new PortfolioReviewBO(p.getId(), p.getPrincipalKey(), p.getTenantKey(), p.getSymbol(), p.getPeriodStart(), p.getPeriodEnd(), p.getThesis(), p.getPlan(), p.getExecution(), p.getLessons(), p.getNextAction(), p.getTags(), p.getRating(), p.getMetricsSnapshot(), p.getStatus()); }
}
