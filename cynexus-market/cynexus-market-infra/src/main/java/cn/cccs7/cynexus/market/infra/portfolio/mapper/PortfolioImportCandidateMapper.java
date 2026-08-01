package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportCandidatePO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PortfolioImportCandidateMapper {
    int deleteByTaskId(long taskId);

    int insert(PortfolioImportCandidatePO candidate);

    List<PortfolioImportCandidatePO> listByTaskId(long taskId);

    int markAccepted(long candidateId, long expectedVersion);
}
