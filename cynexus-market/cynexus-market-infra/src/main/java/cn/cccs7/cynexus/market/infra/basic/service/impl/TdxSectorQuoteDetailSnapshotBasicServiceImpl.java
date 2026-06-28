package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxSectorQuoteDetailSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TdxSectorQuoteDetailSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TdxSectorQuoteDetailSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class TdxSectorQuoteDetailSnapshotBasicServiceImpl extends AbstractCrudBasicService<TdxSectorQuoteDetailSnapshotPO>
        implements TdxSectorQuoteDetailSnapshotBasicService {

    public TdxSectorQuoteDetailSnapshotBasicServiceImpl(TdxSectorQuoteDetailSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
