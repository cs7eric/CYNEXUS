package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobCategoryMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobCategoryBasicServiceImpl implements SchedulerJobCategoryBasicService {

    private final SchedulerJobCategoryMapper schedulerJobCategoryMapper;

    public SchedulerJobCategoryBasicServiceImpl(SchedulerJobCategoryMapper schedulerJobCategoryMapper) {
        this.schedulerJobCategoryMapper = schedulerJobCategoryMapper;
    }

    @Override
    public List<SchedulerJobCategoryPO> listAll() {
        return schedulerJobCategoryMapper.selectList(null);
    }
}
