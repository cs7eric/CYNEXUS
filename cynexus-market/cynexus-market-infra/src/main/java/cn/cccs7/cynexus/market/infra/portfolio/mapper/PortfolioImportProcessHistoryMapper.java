package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportProcessHistoryPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioImportProcessHistoryMapper {
    int append(PortfolioImportProcessHistoryPO history);

    PortfolioImportProcessHistoryPO findByTaskId(String principalKey, String tenantKey, long taskId);
}
