package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiExecutionLogPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AiExecutionLogMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AiExecutionLogBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class AiExecutionLogBasicServiceImpl implements AiExecutionLogBasicService {

    private final AiExecutionLogMapper aiExecutionLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public AiExecutionLogBasicServiceImpl(AiExecutionLogMapper aiExecutionLogMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.aiExecutionLogMapper = aiExecutionLogMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Long create(AiExecutionLogPO log) {
        if (log.getId() == null) {
            log.setId(snowflakeIdGenerator.nextId());
        }
        aiExecutionLogMapper.insert(log);
        return log.getId();
    }
}
