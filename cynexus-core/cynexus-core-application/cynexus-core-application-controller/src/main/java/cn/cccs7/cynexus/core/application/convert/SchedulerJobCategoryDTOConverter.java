package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobCategoryDTOConverter extends BaseMapper<SchedulerJobCategoryDTO, SchedulerJobCategoryBO> {

    SchedulerJobCategoryDTOConverter INSTANCE = Mappers.getMapper(SchedulerJobCategoryDTOConverter.class);
}
