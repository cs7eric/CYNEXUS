package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationAnalysisDailySnapshotMapper extends BaseMapperX<ApplicationAnalysisDailySnapshotPO> {

    List<ApplicationAnalysisDailySnapshotPO> selectRecent30ByTargetKey(@Param("targetKey") String targetKey,
                                                                       @Param("limit") int limit);

    ApplicationAnalysisDailySnapshotPO selectRecent30ByTargetKeyAndDate(@Param("targetKey") String targetKey,
                                                                        @Param("tradeDate") LocalDate tradeDate);
}
