package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryPO;
import java.util.List;

public interface SchedulerJobCategoryBasicService {

    List<SchedulerJobCategoryPO> listAll();

    SchedulerJobCategoryPO getById(Long id);

    Long create(SchedulerJobCategoryPO po);

    boolean update(SchedulerJobCategoryPO po);

    boolean delete(Long id);
}
