package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobRunHistoryPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobRunHistoryMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobRunHistoryBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobRunHistoryBasicServiceImpl implements SchedulerJobRunHistoryBasicService {

    private final SchedulerJobRunHistoryMapper schedulerJobRunHistoryMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SchedulerJobRunHistoryBasicServiceImpl(SchedulerJobRunHistoryMapper schedulerJobRunHistoryMapper,
                                                  SnowflakeIdGenerator snowflakeIdGenerator) {
        this.schedulerJobRunHistoryMapper = schedulerJobRunHistoryMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<SchedulerJobRunHistoryPO> listAll() {
        return schedulerJobRunHistoryMapper.selectList(null);
    }

    @Override
    public SchedulerJobRunHistoryPO getById(Long id) {
        return schedulerJobRunHistoryMapper.selectById(id);
    }

    @Override
    public List<SchedulerJobRunHistoryPO> listByJobId(Long jobId, Integer limit) {
        return schedulerJobRunHistoryMapper.selectByJobId(jobId, limit);
    }

    @Override
    public List<SchedulerJobRunHistoryPO> listSince(OffsetDateTime startedAt) {
        return schedulerJobRunHistoryMapper.selectSince(startedAt);
    }

    @Override
    public Long create(SchedulerJobRunHistoryPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        schedulerJobRunHistoryMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(SchedulerJobRunHistoryPO po) {
        return schedulerJobRunHistoryMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return schedulerJobRunHistoryMapper.deleteById(id) > 0;
    }
}
