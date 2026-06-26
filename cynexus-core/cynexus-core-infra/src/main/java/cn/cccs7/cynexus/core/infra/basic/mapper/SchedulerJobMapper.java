package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SchedulerJobMapper extends BaseMapperX<SchedulerJobPO> {
}
