package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioRealizedPnlBaselinePO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioRealizedPnlBaselineMapper {
    List<PortfolioRealizedPnlBaselinePO> list(String principalKey, String tenantKey);
}
