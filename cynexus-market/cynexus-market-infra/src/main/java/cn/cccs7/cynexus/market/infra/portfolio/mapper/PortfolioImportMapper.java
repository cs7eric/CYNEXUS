package cn.cccs7.cynexus.market.infra.portfolio.mapper;

import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportAssetPO;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PortfolioImportMapper {
    int insertAsset(PortfolioImportAssetPO asset);

    PortfolioImportAssetPO findAssetById(long assetId);

    int insertTask(PortfolioImportTaskPO task);

    int updateTask(PortfolioImportTaskPO task);

    PortfolioImportTaskPO findTaskByOwnerAndId(@Param("principalKey") String principalKey,
                                               @Param("tenantKey") String tenantKey,
                                               @Param("taskId") long taskId);

    PortfolioImportTaskPO findTaskByIdempotency(@Param("principalKey") String principalKey,
                                                @Param("tenantKey") String tenantKey,
                                                @Param("idempotencyKey") String idempotencyKey);
}
