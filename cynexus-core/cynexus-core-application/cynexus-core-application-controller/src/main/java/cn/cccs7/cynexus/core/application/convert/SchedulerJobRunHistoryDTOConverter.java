package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobRunHistoryDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SchedulerJobRunHistoryDTOConverter extends BaseMapper<SchedulerJobRunHistoryDTO, SchedulerJobRunHistoryBO> {

    SchedulerJobRunHistoryDTOConverter INSTANCE = Mappers.getMapper(SchedulerJobRunHistoryDTOConverter.class);
}
