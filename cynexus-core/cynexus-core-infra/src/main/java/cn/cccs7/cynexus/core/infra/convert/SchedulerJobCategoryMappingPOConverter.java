package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryMappingPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobCategoryMappingPOConverter extends BaseMapper<SchedulerJobCategoryMappingPO, SchedulerJobCategoryMappingBO> {

    SchedulerJobCategoryMappingPOConverter INSTANCE = Mappers.getMapper(SchedulerJobCategoryMappingPOConverter.class);
}
