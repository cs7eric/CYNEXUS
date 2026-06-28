package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.HeatmapSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.HeatmapSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.HeatmapSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class HeatmapSnapshotBasicServiceImpl extends AbstractCrudBasicService<HeatmapSnapshotPO>
        implements HeatmapSnapshotBasicService {

    public HeatmapSnapshotBasicServiceImpl(HeatmapSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
