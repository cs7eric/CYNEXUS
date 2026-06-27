package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.ProfitEffectDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.ProfitEffectDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.ProfitEffectDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class ProfitEffectDailyBasicServiceImpl extends AbstractCrudBasicService<ProfitEffectDailyPO>
        implements ProfitEffectDailyBasicService {

    public ProfitEffectDailyBasicServiceImpl(ProfitEffectDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
