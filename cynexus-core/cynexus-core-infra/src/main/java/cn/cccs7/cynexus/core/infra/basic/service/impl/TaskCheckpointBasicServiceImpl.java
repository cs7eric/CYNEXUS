package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskCheckpointPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.TaskCheckpointMapper;
import cn.cccs7.cynexus.core.infra.basic.service.TaskCheckpointBasicService;
import org.springframework.stereotype.Service;

@Service
public class TaskCheckpointBasicServiceImpl extends AbstractCrudBasicService<TaskCheckpointPO>
        implements TaskCheckpointBasicService {

    public TaskCheckpointBasicServiceImpl(TaskCheckpointMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
