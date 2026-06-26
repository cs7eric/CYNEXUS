package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseSectorDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketPulseSectorDailySnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseSectorDailySnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseSectorDailySnapshotBasicServiceImpl
        extends AbstractCrudBasicService<MarketPulseSectorDailySnapshotPO>
        implements MarketPulseSectorDailySnapshotBasicService {

    public MarketPulseSectorDailySnapshotBasicServiceImpl(MarketPulseSectorDailySnapshotMapper mapper,
                                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
