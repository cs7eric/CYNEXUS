package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobDTOConverter extends BaseMapper<SchedulerJobDTO, SchedulerJobBO> {

    SchedulerJobDTOConverter INSTANCE = Mappers.getMapper(SchedulerJobDTOConverter.class);
}
