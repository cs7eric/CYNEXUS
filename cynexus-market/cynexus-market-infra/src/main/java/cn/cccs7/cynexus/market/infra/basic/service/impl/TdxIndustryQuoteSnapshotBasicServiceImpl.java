package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxIndustryQuoteSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TdxIndustryQuoteSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TdxIndustryQuoteSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteSnapshotBasicServiceImpl extends AbstractCrudBasicService<TdxIndustryQuoteSnapshotPO>
        implements TdxIndustryQuoteSnapshotBasicService {

    public TdxIndustryQuoteSnapshotBasicServiceImpl(TdxIndustryQuoteSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
