package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioAccountRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioAccountPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioAccountMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioAccountRepositoryAdapter implements PortfolioAccountRepository {
    private final PortfolioAccountMapper mapper;

    public PortfolioAccountRepositoryAdapter(PortfolioAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PortfolioAccountBO find(String principalKey, String tenantKey) {
        PortfolioAccountPO value = mapper.find(principalKey, tenantKey);
        return value == null ? null : toBo(value);
    }

    @Override
    public PortfolioAccountBO save(String principalKey, String tenantKey, BigDecimal totalAssets,
                                   OffsetDateTime updatedAt) {
        PortfolioAccountPO value = new PortfolioAccountPO();
        value.setPrincipalKey(principalKey);
        value.setTenantKey(tenantKey);
        value.setTotalAssets(totalAssets);
        value.setUpdatedAt(updatedAt);
        mapper.upsert(value);
        return toBo(value);
    }

    private PortfolioAccountBO toBo(PortfolioAccountPO value) {
        return new PortfolioAccountBO(value.getTotalAssets(), value.getAvailableCash(), value.getUpdatedAt());
    }
}
