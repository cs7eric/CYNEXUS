package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioOutboxEventPO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioOutboxMapper {
    int insert(PortfolioOutboxEventPO event);

    List<PortfolioOutboxEventPO> claimReady(OffsetDateTime now, int limit, String leaseOwner,
                                            OffsetDateTime leaseUntil);

    int markPublished(long id, String leaseOwner, OffsetDateTime publishedAt);

    int markRetry(long id, String leaseOwner, OffsetDateTime availableAt,
                  String errorCode, String errorMessage);

    int markFailed(long id, String leaseOwner, String errorCode, String errorMessage);
}
