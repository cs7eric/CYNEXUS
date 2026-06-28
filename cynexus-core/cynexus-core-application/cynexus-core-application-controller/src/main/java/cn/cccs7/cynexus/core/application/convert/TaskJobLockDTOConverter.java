package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.api.dto.TaskJobLockDTO;
import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskJobLockDTOConverter extends BaseMapper<TaskJobLockDTO, TaskJobLockBO> {

    TaskJobLockDTOConverter INSTANCE = Mappers.getMapper(TaskJobLockDTOConverter.class);
}
