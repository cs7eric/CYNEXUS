package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobStatusMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobStatusBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobStatusBasicServiceImpl implements SchedulerJobStatusBasicService {

    private final SchedulerJobStatusMapper schedulerJobStatusMapper;

    public SchedulerJobStatusBasicServiceImpl(SchedulerJobStatusMapper schedulerJobStatusMapper) {
        this.schedulerJobStatusMapper = schedulerJobStatusMapper;
    }

    @Override
    public List<SchedulerJobStatusPO> listAll() {
        return schedulerJobStatusMapper.selectList(null);
    }
}
