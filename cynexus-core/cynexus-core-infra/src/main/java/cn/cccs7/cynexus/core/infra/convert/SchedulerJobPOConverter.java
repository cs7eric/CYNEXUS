package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobPOConverter extends BaseMapper<SchedulerJobPO, SchedulerJobBO> {

    SchedulerJobPOConverter INSTANCE = Mappers.getMapper(SchedulerJobPOConverter.class);
}
