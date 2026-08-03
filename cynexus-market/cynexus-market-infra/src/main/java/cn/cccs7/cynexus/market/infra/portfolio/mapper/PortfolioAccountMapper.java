package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioAccountPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioAccountMapper {
    PortfolioAccountPO find(String principalKey, String tenantKey);

    int upsert(PortfolioAccountPO account);
}
