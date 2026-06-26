package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobStatusPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobStatusPOConverter extends BaseMapper<SchedulerJobStatusPO, SchedulerJobStatusBO> {

    SchedulerJobStatusPOConverter INSTANCE = Mappers.getMapper(SchedulerJobStatusPOConverter.class);
}
