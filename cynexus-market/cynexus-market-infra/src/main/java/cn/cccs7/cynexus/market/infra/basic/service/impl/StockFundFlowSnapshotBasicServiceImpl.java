package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockFundFlowSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowSnapshotBasicServiceImpl extends AbstractCrudBasicService<StockFundFlowSnapshotPO>
        implements StockFundFlowSnapshotBasicService {

    public StockFundFlowSnapshotBasicServiceImpl(StockFundFlowSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
