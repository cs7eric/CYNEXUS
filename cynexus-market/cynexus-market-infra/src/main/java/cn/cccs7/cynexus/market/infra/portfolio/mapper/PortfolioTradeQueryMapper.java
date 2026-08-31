package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioTradeQueryMapper {
    List<PortfolioTradePO> list(@Param("principalKey") String principalKey,
                                @Param("tenantKey") String tenantKey,
                                @Param("symbol") String symbol);

    PortfolioTradePO findById(@Param("principalKey") String principalKey, @Param("tenantKey") String tenantKey,
                              @Param("tradeId") long tradeId);
}
