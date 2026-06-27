package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MaCountDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MaCountDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MaCountDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class MaCountDailyBasicServiceImpl extends AbstractCrudBasicService<MaCountDailyPO>
        implements MaCountDailyBasicService {

    public MaCountDailyBasicServiceImpl(MaCountDailyMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
