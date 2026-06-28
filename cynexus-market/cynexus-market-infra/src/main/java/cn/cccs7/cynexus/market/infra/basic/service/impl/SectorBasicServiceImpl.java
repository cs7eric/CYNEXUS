package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SectorMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SectorBasicService;
import org.springframework.stereotype.Service;

@Service
public class SectorBasicServiceImpl extends AbstractCrudBasicService<SectorPO>
        implements SectorBasicService {

    public SectorBasicServiceImpl(SectorMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
