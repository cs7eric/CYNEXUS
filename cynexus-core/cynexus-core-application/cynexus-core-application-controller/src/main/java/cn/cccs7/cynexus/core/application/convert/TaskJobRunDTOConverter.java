package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.api.dto.TaskJobRunDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskJobRunDTOConverter extends BaseMapper<TaskJobRunDTO, TaskJobRunBO> {

    TaskJobRunDTOConverter INSTANCE = Mappers.getMapper(TaskJobRunDTOConverter.class);
}
