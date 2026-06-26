package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobRunHistoryPO;
import java.util.List;

public interface SchedulerJobRunHistoryBasicService {

    List<SchedulerJobRunHistoryPO> listAll();
}
