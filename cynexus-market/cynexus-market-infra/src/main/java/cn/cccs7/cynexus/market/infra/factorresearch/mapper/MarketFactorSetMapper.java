package cn.cccs7.cynexus.market.infra.factorresearch.mapper;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorSetPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper public interface MarketFactorSetMapper {
    MarketFactorSetPO find(@Param("tenantId") String tenantId,@Param("ownerId") String ownerId,@Param("code") String code);
    List<MarketFactorSetPO> list(@Param("tenantId") String tenantId,@Param("ownerId") String ownerId,@Param("offset") int offset,@Param("limit") int limit);
    int insertDefinition(@Param("d") MarketFactorSetPO d);
    int updateDefinition(@Param("d") MarketFactorSetPO d,@Param("expectedVersion") long expectedVersion);
    int insertVersion(@Param("v") MarketFactorSetPO v,@Param("definitionId") long definitionId);
    int updateVersionStatus(@Param("definitionId") long definitionId,@Param("versionNo") int versionNo,@Param("status") String status,@Param("snapshot") String snapshot);
    int activateVersion(@Param("definitionId") long definitionId,@Param("tenantId") String tenantId,@Param("ownerId") String ownerId,@Param("versionId") long versionId);
}
