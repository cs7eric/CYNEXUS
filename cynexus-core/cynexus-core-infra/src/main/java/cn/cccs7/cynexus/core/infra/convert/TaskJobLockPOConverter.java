package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.TaskJobLockBO;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskJobLockPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskJobLockPOConverter extends BaseMapper<TaskJobLockPO, TaskJobLockBO> {

    TaskJobLockPOConverter INSTANCE = Mappers.getMapper(TaskJobLockPOConverter.class);
}
