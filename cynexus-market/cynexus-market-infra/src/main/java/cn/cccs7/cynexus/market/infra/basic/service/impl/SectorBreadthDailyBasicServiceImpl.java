package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorBreadthDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SectorBreadthDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SectorBreadthDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class SectorBreadthDailyBasicServiceImpl extends AbstractCrudBasicService<SectorBreadthDailyPO>
        implements SectorBreadthDailyBasicService {

    public SectorBreadthDailyBasicServiceImpl(SectorBreadthDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
