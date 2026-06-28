package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskJobLockPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.TaskJobLockMapper;
import cn.cccs7.cynexus.core.infra.basic.service.TaskJobLockBasicService;
import org.springframework.stereotype.Service;

@Service
public class TaskJobLockBasicServiceImpl extends AbstractCrudBasicService<TaskJobLockPO>
        implements TaskJobLockBasicService {

    public TaskJobLockBasicServiceImpl(TaskJobLockMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
