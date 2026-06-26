package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.infra.basic.entity.SchedulerJobRunHistoryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobRunHistoryPOConverter extends BaseMapper<SchedulerJobRunHistoryPO, SchedulerJobRunHistoryBO> {

    SchedulerJobRunHistoryPOConverter INSTANCE = Mappers.getMapper(SchedulerJobRunHistoryPOConverter.class);
}
