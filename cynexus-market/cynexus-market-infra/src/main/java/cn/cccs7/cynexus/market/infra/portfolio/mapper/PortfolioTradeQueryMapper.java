package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioTradeQueryMapper {
    List<PortfolioTradePO> list(String principalKey, String tenantKey, String symbol);
}
