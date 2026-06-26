package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SelfSelectedListPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SelfSelectedListMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SelfSelectedListBasicService;
import org.springframework.stereotype.Service;

@Service
public class SelfSelectedListBasicServiceImpl extends AbstractCrudBasicService<SelfSelectedListPO>
        implements SelfSelectedListBasicService {

    public SelfSelectedListBasicServiceImpl(SelfSelectedListMapper mapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
