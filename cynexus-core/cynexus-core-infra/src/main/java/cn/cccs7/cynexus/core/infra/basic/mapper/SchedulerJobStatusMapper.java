package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SchedulerJobStatusMapper extends BaseMapperX<SchedulerJobStatusPO> {

    SchedulerJobStatusPO selectByJobId(@Param("jobId") Long jobId);

    int updateEnabledById(@Param("id") Long id, @Param("enabled") Boolean enabled);

    int updateRunResultById(@Param("id") Long id,
                            @Param("lastRunAt") OffsetDateTime lastRunAt,
                            @Param("lastRunOk") Boolean lastRunOk,
                            @Param("lastRunStatus") String lastRunStatus,
                            @Param("lastRunErrorMessage") String lastRunErrorMessage,
                            @Param("lastDurationSeconds") BigDecimal lastDurationSeconds);
}
