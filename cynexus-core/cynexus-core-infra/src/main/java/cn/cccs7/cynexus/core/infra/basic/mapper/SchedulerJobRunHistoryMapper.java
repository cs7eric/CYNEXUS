package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobRunHistoryPO;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SchedulerJobRunHistoryMapper extends BaseMapperX<SchedulerJobRunHistoryPO> {

    List<SchedulerJobRunHistoryPO> selectByJobId(@Param("jobId") Long jobId, @Param("limit") Integer limit);

    List<SchedulerJobRunHistoryPO> selectSince(@Param("startedAt") OffsetDateTime startedAt);

    int updateDetailById(@Param("po") SchedulerJobRunHistoryPO po);
}
