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

    int updateRunningById(@Param("id") Long id,
                          @Param("running") Boolean running,
                          @Param("schedulerStartedAt") OffsetDateTime schedulerStartedAt,
                          @Param("stoppedAt") OffsetDateTime stoppedAt);

    int updateRunResultById(@Param("id") Long id,
                            @Param("lastRunAt") OffsetDateTime lastRunAt,
                            @Param("lastRunOk") Boolean lastRunOk,
                            @Param("lastRunStatus") String lastRunStatus,
                            @Param("lastRunErrorMessage") String lastRunErrorMessage,
                            @Param("lastDurationSeconds") BigDecimal lastDurationSeconds);

    int updateRunResultDetailById(@Param("id") Long id,
                                  @Param("lastRunAt") OffsetDateTime lastRunAt,
                                  @Param("lastRunOk") Boolean lastRunOk,
                                  @Param("lastRunStatus") String lastRunStatus,
                                  @Param("lastRunErrorMessage") String lastRunErrorMessage,
                                  @Param("lastDurationSeconds") BigDecimal lastDurationSeconds,
                                  @Param("lastTargetsProcessed") Integer lastTargetsProcessed,
                                  @Param("progress") String progress,
                                  @Param("extra") String extra,
                                  @Param("remark") String remark);

    int updateProgressById(@Param("id") Long id,
                           @Param("running") Boolean running,
                           @Param("lastRunAt") OffsetDateTime lastRunAt,
                           @Param("lastRunStatus") String lastRunStatus,
                           @Param("progress") String progress,
                           @Param("extra") String extra,
                           @Param("remark") String remark);
}
