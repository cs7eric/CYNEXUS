package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SectorFundFlowDailySnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class SectorFundFlowDailySnapshotBasicServiceImpl
        extends AbstractCrudBasicService<SectorFundFlowDailySnapshotPO>
        implements SectorFundFlowDailySnapshotBasicService {

    public SectorFundFlowDailySnapshotBasicServiceImpl(SectorFundFlowDailySnapshotMapper mapper,
                                                       SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
