package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioReviewPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioReviewMapper {
    int insert(PortfolioReviewPO review);
    int update(PortfolioReviewPO review);
    PortfolioReviewPO findById(String principalKey, String tenantKey, long id);
    List<PortfolioReviewPO> list(String principalKey, String tenantKey, String symbol);
}
