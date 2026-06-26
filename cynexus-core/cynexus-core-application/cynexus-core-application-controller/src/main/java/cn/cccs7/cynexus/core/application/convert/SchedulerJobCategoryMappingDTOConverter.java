package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryMappingDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobCategoryMappingDTOConverter extends BaseMapper<SchedulerJobCategoryMappingDTO, SchedulerJobCategoryMappingBO> {

    SchedulerJobCategoryMappingDTOConverter INSTANCE = Mappers.getMapper(SchedulerJobCategoryMappingDTOConverter.class);
}
