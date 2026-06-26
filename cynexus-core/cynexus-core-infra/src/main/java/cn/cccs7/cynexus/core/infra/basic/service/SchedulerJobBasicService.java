package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobPO;
import java.util.List;

public interface SchedulerJobBasicService {

    List<SchedulerJobPO> listAll();

    SchedulerJobPO getById(Long id);

    Long create(SchedulerJobPO po);

    boolean update(SchedulerJobPO po);

    boolean delete(Long id);
}
