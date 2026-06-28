package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.TaskJobRunBO;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskJobRunPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskJobRunPOConverter extends BaseMapper<TaskJobRunPO, TaskJobRunBO> {

    TaskJobRunPOConverter INSTANCE = Mappers.getMapper(TaskJobRunPOConverter.class);
}
