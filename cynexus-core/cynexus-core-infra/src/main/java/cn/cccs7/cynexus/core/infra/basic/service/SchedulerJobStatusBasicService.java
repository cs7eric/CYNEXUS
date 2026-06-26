package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import java.util.List;

public interface SchedulerJobStatusBasicService {

    List<SchedulerJobStatusPO> listAll();
}
