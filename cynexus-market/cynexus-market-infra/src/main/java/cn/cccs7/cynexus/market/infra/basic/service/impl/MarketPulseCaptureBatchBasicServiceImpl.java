package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketPulseCaptureBatchMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseCaptureBatchBasicService;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseCaptureBatchBasicServiceImpl extends AbstractCrudBasicService<MarketPulseCaptureBatchPO>
        implements MarketPulseCaptureBatchBasicService {

    public MarketPulseCaptureBatchBasicServiceImpl(MarketPulseCaptureBatchMapper mapper,
                                                   SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
