package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobStatusDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobStatusDTOConverter extends BaseMapper<SchedulerJobStatusDTO, SchedulerJobStatusBO> {

    SchedulerJobStatusDTOConverter INSTANCE = Mappers.getMapper(SchedulerJobStatusDTOConverter.class);
}
