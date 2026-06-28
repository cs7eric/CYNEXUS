package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockFundFlowCaptureBatchMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowCaptureBatchBasicService;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowCaptureBatchBasicServiceImpl extends AbstractCrudBasicService<StockFundFlowCaptureBatchPO>
        implements StockFundFlowCaptureBatchBasicService {

    public StockFundFlowCaptureBatchBasicServiceImpl(StockFundFlowCaptureBatchMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
