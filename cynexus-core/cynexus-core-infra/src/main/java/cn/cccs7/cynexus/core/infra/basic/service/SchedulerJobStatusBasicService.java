package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface SchedulerJobStatusBasicService {

    List<SchedulerJobStatusPO> listAll();

    SchedulerJobStatusPO getById(Long id);

    SchedulerJobStatusPO getByJobId(Long jobId);

    Long create(SchedulerJobStatusPO po);

    boolean update(SchedulerJobStatusPO po);

    boolean updateEnabled(Long id, Boolean enabled);

    boolean updateRunResult(Long id,
                            OffsetDateTime lastRunAt,
                            Boolean lastRunOk,
                            String lastRunStatus,
                            String lastRunErrorMessage,
                            BigDecimal lastDurationSeconds);

    boolean delete(Long id);
}
