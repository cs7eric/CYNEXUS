package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationAnalysisResultHistoryMapper extends BaseMapperX<ApplicationAnalysisResultHistoryPO> {

    List<ApplicationAnalysisResultHistoryPO> selectActiveByTargetKey(@Param("targetKey") String targetKey,
                                                                      @Param("limit") int limit);
}
