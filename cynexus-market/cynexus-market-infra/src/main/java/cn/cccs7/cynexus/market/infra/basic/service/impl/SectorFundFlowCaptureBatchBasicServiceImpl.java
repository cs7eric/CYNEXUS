package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SectorFundFlowCaptureBatchMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowCaptureBatchBasicService;
import org.springframework.stereotype.Service;

@Service
public class SectorFundFlowCaptureBatchBasicServiceImpl
        extends AbstractCrudBasicService<SectorFundFlowCaptureBatchPO>
        implements SectorFundFlowCaptureBatchBasicService {

    public SectorFundFlowCaptureBatchBasicServiceImpl(SectorFundFlowCaptureBatchMapper mapper,
                                                      SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
