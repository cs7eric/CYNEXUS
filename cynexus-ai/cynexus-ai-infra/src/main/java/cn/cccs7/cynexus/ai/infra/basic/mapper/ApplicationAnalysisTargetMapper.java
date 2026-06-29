package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationAnalysisTargetMapper extends BaseMapperX<ApplicationAnalysisTargetPO> {

    List<ApplicationAnalysisTargetPO> selectActiveTargets();

    ApplicationAnalysisTargetPO selectActiveByTargetKey(@Param("targetKey") String targetKey);

    int softDeleteMissing(@Param("targetKeys") List<String> targetKeys, @Param("deletedAt") OffsetDateTime deletedAt);
}
