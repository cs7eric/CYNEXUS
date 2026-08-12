package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PortfolioTradeMapper {
    int insert(PortfolioTradePO trade);

    int update(@Param("principalKey") String principalKey, @Param("tenantKey") String tenantKey,
               @Param("trade") PortfolioTradePO trade);
}
