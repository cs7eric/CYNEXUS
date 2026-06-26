package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobCategoryMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobCategoryBasicServiceImpl implements SchedulerJobCategoryBasicService {

    private final SchedulerJobCategoryMapper schedulerJobCategoryMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SchedulerJobCategoryBasicServiceImpl(SchedulerJobCategoryMapper schedulerJobCategoryMapper,
                                                SnowflakeIdGenerator snowflakeIdGenerator) {
        this.schedulerJobCategoryMapper = schedulerJobCategoryMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<SchedulerJobCategoryPO> listAll() {
        return schedulerJobCategoryMapper.selectList(null);
    }

    @Override
    public SchedulerJobCategoryPO getById(Long id) {
        return schedulerJobCategoryMapper.selectById(id);
    }

    @Override
    public Long create(SchedulerJobCategoryPO po) {
        if (po.getId() == null) {
            po.setId(snowflakeIdGenerator.nextId());
        }
        schedulerJobCategoryMapper.insert(po);
        return po.getId();
    }

    @Override
    public boolean update(SchedulerJobCategoryPO po) {
        return schedulerJobCategoryMapper.updateById(po) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return schedulerJobCategoryMapper.deleteById(id) > 0;
    }
}
