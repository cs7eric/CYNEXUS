package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockBasicMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockBasicBasicService;
import org.springframework.stereotype.Service;

@Service
public class StockBasicBasicServiceImpl extends AbstractCrudBasicService<StockBasicPO>
        implements StockBasicBasicService {

    public StockBasicBasicServiceImpl(StockBasicMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
