package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockSectorMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockSectorBasicService;
import org.springframework.stereotype.Service;

@Service
public class StockSectorBasicServiceImpl extends AbstractCrudBasicService<StockSectorPO>
        implements StockSectorBasicService {

    public StockSectorBasicServiceImpl(StockSectorMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
