package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleSectorSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StyleSectorSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StyleSectorSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class StyleSectorSnapshotBasicServiceImpl extends AbstractCrudBasicService<StyleSectorSnapshotPO>
        implements StyleSectorSnapshotBasicService {

    public StyleSectorSnapshotBasicServiceImpl(StyleSectorSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
