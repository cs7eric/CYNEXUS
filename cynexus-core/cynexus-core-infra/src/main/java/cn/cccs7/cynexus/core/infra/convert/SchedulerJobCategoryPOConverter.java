package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobCategoryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobCategoryPOConverter extends BaseMapper<SchedulerJobCategoryPO, SchedulerJobCategoryBO> {

    SchedulerJobCategoryPOConverter INSTANCE = Mappers.getMapper(SchedulerJobCategoryPOConverter.class);
}
