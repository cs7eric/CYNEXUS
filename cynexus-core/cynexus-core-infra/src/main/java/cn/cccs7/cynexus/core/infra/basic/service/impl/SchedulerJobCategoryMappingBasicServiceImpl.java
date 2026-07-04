package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryMappingPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobCategoryMappingMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryMappingBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobCategoryMappingBasicServiceImpl implements SchedulerJobCategoryMappingBasicService {

    private final SchedulerJobCategoryMappingMapper schedulerJobCategoryMappingMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SchedulerJobCategoryMappingBasicServiceImpl(SchedulerJobCategoryMappingMapper schedulerJobCategoryMappingMapper,
                                                       SnowflakeIdGenerator snowflakeIdGenerator) {
        this.schedulerJobCategoryMappingMapper = schedulerJobCategoryMappingMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<SchedulerJobCategoryMappingPO> listAll() {
        return schedulerJobCategoryMappingMapper.selectList(null);
    }

    @Override
    public SchedulerJobCategoryMappingPO getById(Long id) {
        return schedulerJobCategoryMappingMapper.selectById(id);
    }

    @Override
    public List<SchedulerJobCategoryMappingPO> listByJobId(Long jobId) {
        return schedulerJobCategoryMappingMapper.selectByJobId(jobId);
    }

    @Override
    public Long create(SchedulerJobCategoryMappingPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        schedulerJobCategoryMappingMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(SchedulerJobCategoryMappingPO po) {
        return schedulerJobCategoryMappingMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return schedulerJobCategoryMappingMapper.deleteById(id) > 0;
    }
}
