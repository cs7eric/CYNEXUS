package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface PortfolioAccountRepository {
    PortfolioAccountBO find(String principalKey, String tenantKey);

    PortfolioAccountBO save(String principalKey, String tenantKey, BigDecimal totalAssets,
                             OffsetDateTime updatedAt);
}
