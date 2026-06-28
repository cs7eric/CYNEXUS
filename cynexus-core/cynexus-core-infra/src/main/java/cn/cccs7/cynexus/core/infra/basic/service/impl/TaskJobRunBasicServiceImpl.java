package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.TaskJobRunPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.TaskJobRunMapper;
import cn.cccs7.cynexus.core.infra.basic.service.TaskJobRunBasicService;
import org.springframework.stereotype.Service;

@Service
public class TaskJobRunBasicServiceImpl extends AbstractCrudBasicService<TaskJobRunPO>
        implements TaskJobRunBasicService {

    public TaskJobRunBasicServiceImpl(TaskJobRunMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
