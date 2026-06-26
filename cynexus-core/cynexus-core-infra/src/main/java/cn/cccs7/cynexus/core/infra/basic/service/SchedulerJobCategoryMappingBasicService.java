package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryMappingPO;
import java.util.List;

public interface SchedulerJobCategoryMappingBasicService {

    List<SchedulerJobCategoryMappingPO> listAll();

    SchedulerJobCategoryMappingPO getById(Long id);

    Long create(SchedulerJobCategoryMappingPO po);

    boolean update(SchedulerJobCategoryMappingPO po);

    boolean delete(Long id);
}
