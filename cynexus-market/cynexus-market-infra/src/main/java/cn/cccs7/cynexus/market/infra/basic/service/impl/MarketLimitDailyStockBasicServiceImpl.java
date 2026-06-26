package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailyStockPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketLimitDailyStockMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketLimitDailyStockBasicService;
import org.springframework.stereotype.Service;

@Service
public class MarketLimitDailyStockBasicServiceImpl extends AbstractCrudBasicService<MarketLimitDailyStockPO>
        implements MarketLimitDailyStockBasicService {

    public MarketLimitDailyStockBasicServiceImpl(MarketLimitDailyStockMapper mapper,
                                                 SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
