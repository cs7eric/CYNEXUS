package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockBasicMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockBasicBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockBasicBasicServiceImpl extends AbstractCrudBasicService<StockBasicPO>
        implements StockBasicBasicService {

    private final StockBasicMapper stockBasicMapper;

    public StockBasicBasicServiceImpl(StockBasicMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.stockBasicMapper = mapper;
    }

    @Override
    public List<StockBasicPO> searchForStockChart(String keyword, int limit) {
        return stockBasicMapper.searchForStockChart(keyword, limit);
    }

    @Override
    public StockBasicPO findByCodeForStockChart(String code) {
        return stockBasicMapper.findByCodeForStockChart(code);
    }
}
