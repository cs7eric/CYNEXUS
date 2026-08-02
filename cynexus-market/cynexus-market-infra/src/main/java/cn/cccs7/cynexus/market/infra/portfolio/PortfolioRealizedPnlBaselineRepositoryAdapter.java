package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioRealizedPnlBaselineRepository;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioRealizedPnlBaselineMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioRealizedPnlBaselineRepositoryAdapter implements PortfolioRealizedPnlBaselineRepository {
    private final PortfolioRealizedPnlBaselineMapper mapper;

    public PortfolioRealizedPnlBaselineRepositoryAdapter(PortfolioRealizedPnlBaselineMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, BigDecimal> list(String principalKey, String tenantKey) {
        return mapper.list(principalKey, tenantKey).stream()
                .collect(Collectors.toUnmodifiableMap(
                        item -> item.getSymbol(),
                        item -> item.getRealizedPnl() == null ? BigDecimal.ZERO : item.getRealizedPnl(),
                        BigDecimal::add));
    }
}
