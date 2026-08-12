package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioReviewAnchorPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioReviewAnchorMapper {
    List<PortfolioReviewAnchorPO> list(String principalKey, String tenantKey, String symbol, LocalDate tradeDate);

    int insert(PortfolioReviewAnchorPO anchor);

    int update(PortfolioReviewAnchorPO anchor);

    int softDelete(String principalKey, String tenantKey, long id);
}
