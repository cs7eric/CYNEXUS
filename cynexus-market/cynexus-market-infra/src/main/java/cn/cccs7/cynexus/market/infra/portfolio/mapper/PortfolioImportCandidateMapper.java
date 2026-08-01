package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportCandidatePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortfolioImportCandidateMapper {
    int deleteByTaskId(long taskId);

    int insert(PortfolioImportCandidatePO candidate);
}
