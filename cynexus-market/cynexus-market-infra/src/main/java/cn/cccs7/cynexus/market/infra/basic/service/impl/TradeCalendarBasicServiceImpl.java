package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TradeCalendarPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TradeCalendarMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TradeCalendarBasicService;
import org.springframework.stereotype.Service;

@Service
public class TradeCalendarBasicServiceImpl extends AbstractCrudBasicService<TradeCalendarPO>
        implements TradeCalendarBasicService {

    public TradeCalendarBasicServiceImpl(TradeCalendarMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
