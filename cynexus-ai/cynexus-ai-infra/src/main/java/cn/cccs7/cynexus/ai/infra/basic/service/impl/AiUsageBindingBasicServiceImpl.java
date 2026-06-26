package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiUsageBindingPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AiUsageBindingMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AiUsageBindingBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class AiUsageBindingBasicServiceImpl extends AbstractCrudBasicService<AiUsageBindingPO>
        implements AiUsageBindingBasicService {

    public AiUsageBindingBasicServiceImpl(AiUsageBindingMapper mapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
