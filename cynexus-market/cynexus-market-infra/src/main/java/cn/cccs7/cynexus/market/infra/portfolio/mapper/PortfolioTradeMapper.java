package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioTradePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioTradeMapper {
    int insert(PortfolioTradePO trade);
}
