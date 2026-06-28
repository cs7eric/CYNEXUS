package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.api.dto.TaskCheckpointDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskCheckpointDTOConverter extends BaseMapper<TaskCheckpointDTO, TaskCheckpointBO> {

    TaskCheckpointDTOConverter INSTANCE = Mappers.getMapper(TaskCheckpointDTOConverter.class);
}
