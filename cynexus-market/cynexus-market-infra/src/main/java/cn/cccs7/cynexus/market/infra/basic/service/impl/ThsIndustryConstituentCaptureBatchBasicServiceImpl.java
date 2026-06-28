package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.ThsIndustryConstituentCaptureBatchMapper;
import cn.cccs7.cynexus.market.infra.basic.service.ThsIndustryConstituentCaptureBatchBasicService;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentCaptureBatchBasicServiceImpl extends AbstractCrudBasicService<ThsIndustryConstituentCaptureBatchPO>
        implements ThsIndustryConstituentCaptureBatchBasicService {

    public ThsIndustryConstituentCaptureBatchBasicServiceImpl(ThsIndustryConstituentCaptureBatchMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
