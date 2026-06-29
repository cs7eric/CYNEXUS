package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobStatusMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobStatusBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobStatusBasicServiceImpl implements SchedulerJobStatusBasicService {

    private final SchedulerJobStatusMapper schedulerJobStatusMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SchedulerJobStatusBasicServiceImpl(SchedulerJobStatusMapper schedulerJobStatusMapper,
                                              SnowflakeIdGenerator snowflakeIdGenerator) {
        this.schedulerJobStatusMapper = schedulerJobStatusMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<SchedulerJobStatusPO> listAll() {
        return schedulerJobStatusMapper.selectList(null);
    }

    @Override
    public SchedulerJobStatusPO getById(Long id) {
        return schedulerJobStatusMapper.selectById(id);
    }

    @Override
    public SchedulerJobStatusPO getByJobId(Long jobId) {
        return schedulerJobStatusMapper.selectByJobId(jobId);
    }

    @Override
    public Long create(SchedulerJobStatusPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        schedulerJobStatusMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(SchedulerJobStatusPO po) {
        return schedulerJobStatusMapper.updateById(po) > 0;
    }

    @Override
    public boolean updateEnabled(Long id, Boolean enabled) {
        return schedulerJobStatusMapper.updateEnabledById(id, enabled) > 0;
    }

    @Override
    public boolean updateRunResult(Long id,
                                   OffsetDateTime lastRunAt,
                                   Boolean lastRunOk,
                                   String lastRunStatus,
                                   String lastRunErrorMessage,
                                   BigDecimal lastDurationSeconds) {
        return schedulerJobStatusMapper.updateRunResultById(
                id,
                lastRunAt,
                lastRunOk,
                lastRunStatus,
                lastRunErrorMessage,
                lastDurationSeconds) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return schedulerJobStatusMapper.deleteById(id) > 0;
    }
}
