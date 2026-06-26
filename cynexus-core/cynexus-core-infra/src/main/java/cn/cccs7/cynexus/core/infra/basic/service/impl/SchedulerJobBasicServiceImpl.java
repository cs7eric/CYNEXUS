package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobBasicServiceImpl implements SchedulerJobBasicService {

    private final SchedulerJobMapper schedulerJobMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SchedulerJobBasicServiceImpl(SchedulerJobMapper schedulerJobMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.schedulerJobMapper = schedulerJobMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<SchedulerJobPO> listAll() {
        return schedulerJobMapper.selectList(null);
    }

    @Override
    public SchedulerJobPO getById(Long id) {
        return schedulerJobMapper.selectById(id);
    }

    @Override
    public Long create(SchedulerJobPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        schedulerJobMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(SchedulerJobPO po) {
        return schedulerJobMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return schedulerJobMapper.deleteById(id) > 0;
    }
}
