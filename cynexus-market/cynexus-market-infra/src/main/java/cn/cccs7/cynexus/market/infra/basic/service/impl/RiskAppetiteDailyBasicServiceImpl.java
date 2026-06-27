package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.RiskAppetiteDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.RiskAppetiteDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.RiskAppetiteDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class RiskAppetiteDailyBasicServiceImpl extends AbstractCrudBasicService<RiskAppetiteDailyPO>
        implements RiskAppetiteDailyBasicService {

    public RiskAppetiteDailyBasicServiceImpl(RiskAppetiteDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
