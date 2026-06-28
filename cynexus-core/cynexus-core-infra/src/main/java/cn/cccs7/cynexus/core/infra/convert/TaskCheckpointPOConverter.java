package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.TaskCheckpointBO;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskCheckpointPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskCheckpointPOConverter extends BaseMapper<TaskCheckpointPO, TaskCheckpointBO> {

    TaskCheckpointPOConverter INSTANCE = Mappers.getMapper(TaskCheckpointPOConverter.class);
}
