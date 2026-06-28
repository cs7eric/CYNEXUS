package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.OverviewDetailSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.OverviewDetailSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.OverviewDetailSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class OverviewDetailSnapshotBasicServiceImpl extends AbstractCrudBasicService<OverviewDetailSnapshotPO>
        implements OverviewDetailSnapshotBasicService {

    public OverviewDetailSnapshotBasicServiceImpl(OverviewDetailSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
