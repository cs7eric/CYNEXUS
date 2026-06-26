package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobRunHistoryPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.SchedulerJobRunHistoryMapper;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobRunHistoryBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerJobRunHistoryBasicServiceImpl implements SchedulerJobRunHistoryBasicService {

    private final SchedulerJobRunHistoryMapper schedulerJobRunHistoryMapper;

    public SchedulerJobRunHistoryBasicServiceImpl(SchedulerJobRunHistoryMapper schedulerJobRunHistoryMapper) {
        this.schedulerJobRunHistoryMapper = schedulerJobRunHistoryMapper;
    }

    @Override
    public List<SchedulerJobRunHistoryPO> listAll() {
        return schedulerJobRunHistoryMapper.selectList(null);
    }
}
