package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.StockTurnoverSeriesSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockTurnoverSeriesSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockTurnoverSeriesSnapshotBasicService;
import org.springframework.stereotype.Service;

@Service
public class StockTurnoverSeriesSnapshotBasicServiceImpl extends AbstractCrudBasicService<StockTurnoverSeriesSnapshotPO>
        implements StockTurnoverSeriesSnapshotBasicService {

    private final StockTurnoverSeriesSnapshotMapper mapper;

    public StockTurnoverSeriesSnapshotBasicServiceImpl(StockTurnoverSeriesSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public StockTurnoverSeriesSnapshotPO findBySymbol(String symbol) {
        return mapper.findBySymbol(symbol);
    }
}
