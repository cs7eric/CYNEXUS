package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.VolatilityDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.VolatilityDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.VolatilityDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class VolatilityDailyBasicServiceImpl extends AbstractCrudBasicService<VolatilityDailyPO>
        implements VolatilityDailyBasicService {

    public VolatilityDailyBasicServiceImpl(VolatilityDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
