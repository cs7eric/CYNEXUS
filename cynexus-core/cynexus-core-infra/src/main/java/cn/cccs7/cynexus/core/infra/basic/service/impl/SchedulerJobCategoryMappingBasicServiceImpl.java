package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryMappingPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobCategoryMappingMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryMappingBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobCategoryMappingBasicServiceImpl implements SchedulerJobCategoryMappingBasicService {

    private final SchedulerJobCategoryMappingMapper schedulerJobCategoryMappingMapper;

    public SchedulerJobCategoryMappingBasicServiceImpl(SchedulerJobCategoryMappingMapper schedulerJobCategoryMappingMapper) {
        this.schedulerJobCategoryMappingMapper = schedulerJobCategoryMappingMapper;
    }

    @Override
    public List<SchedulerJobCategoryMappingPO> listAll() {
        return schedulerJobCategoryMappingMapper.selectList(null);
    }
}
