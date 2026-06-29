package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MsiIndexDailyPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MsiIndexDailyMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MsiIndexDailyBasicService;
import org.springframework.stereotype.Service;

@Service
public class MsiIndexDailyBasicServiceImpl extends AbstractCrudBasicService<MsiIndexDailyPO>
        implements MsiIndexDailyBasicService {

    public MsiIndexDailyBasicServiceImpl(MsiIndexDailyMapper mapper,
                                         SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
