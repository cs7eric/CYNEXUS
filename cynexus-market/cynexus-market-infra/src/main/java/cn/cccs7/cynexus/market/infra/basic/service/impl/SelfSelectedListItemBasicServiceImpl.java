package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SelfSelectedListItemPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SelfSelectedListItemMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SelfSelectedListItemBasicService;
import org.springframework.stereotype.Service;

@Service
public class SelfSelectedListItemBasicServiceImpl extends AbstractCrudBasicService<SelfSelectedListItemPO>
        implements SelfSelectedListItemBasicService {

    public SelfSelectedListItemBasicServiceImpl(SelfSelectedListItemMapper mapper,
                                                SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
