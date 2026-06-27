package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleRiskDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StyleRiskDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StyleRiskDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class StyleRiskDailyBasicServiceImpl extends AbstractCrudBasicService<StyleRiskDailyPO>
        implements StyleRiskDailyBasicService {

    public StyleRiskDailyBasicServiceImpl(StyleRiskDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
