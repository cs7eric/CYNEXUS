package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketLimitDailySnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketLimitDailySnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class MarketLimitDailySnapshotBasicServiceImpl extends AbstractCrudBasicService<MarketLimitDailySnapshotPO>
        implements MarketLimitDailySnapshotBasicService {

    public MarketLimitDailySnapshotBasicServiceImpl(MarketLimitDailySnapshotMapper mapper,
                                                    SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
