package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioOutboxEventPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioOutboxMapper {
    int insert(PortfolioOutboxEventPO event);
}
