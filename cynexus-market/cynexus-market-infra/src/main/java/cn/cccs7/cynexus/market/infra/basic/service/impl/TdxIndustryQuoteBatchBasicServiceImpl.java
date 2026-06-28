package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxIndustryQuoteBatchPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TdxIndustryQuoteBatchMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TdxIndustryQuoteBatchBasicService;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteBatchBasicServiceImpl extends AbstractCrudBasicService<TdxIndustryQuoteBatchPO>
        implements TdxIndustryQuoteBatchBasicService {

    public TdxIndustryQuoteBatchBasicServiceImpl(TdxIndustryQuoteBatchMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
