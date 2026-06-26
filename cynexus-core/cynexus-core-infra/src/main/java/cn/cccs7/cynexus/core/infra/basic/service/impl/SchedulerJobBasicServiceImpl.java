package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobBasicServiceImpl implements SchedulerJobBasicService {

    private final SchedulerJobMapper schedulerJobMapper;

    public SchedulerJobBasicServiceImpl(SchedulerJobMapper schedulerJobMapper) {
        this.schedulerJobMapper = schedulerJobMapper;
    }

    @Override
    public List<SchedulerJobPO> listAll() {
        return schedulerJobMapper.selectList(null);
    }
}
