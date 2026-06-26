package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobStatusMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobStatusBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
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
    public boolean delete(Long id) {
        return schedulerJobStatusMapper.deleteById(id) > 0;
    }
}
