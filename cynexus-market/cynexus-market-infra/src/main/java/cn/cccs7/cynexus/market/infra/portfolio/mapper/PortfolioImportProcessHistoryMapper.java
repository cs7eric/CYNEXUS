package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportProcessHistoryPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioImportProcessHistoryMapper {
    int insert(PortfolioImportProcessHistoryPO history);

    List<PortfolioImportProcessHistoryPO> listByTaskId(String principalKey, String tenantKey, long taskId);
}
