package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.SectorFundFlowDailySnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorFundFlowDailySnapshotBasicServiceImpl
        extends AbstractCrudBasicService<SectorFundFlowDailySnapshotPO>
        implements SectorFundFlowDailySnapshotBasicService {

    private final SectorFundFlowDailySnapshotMapper mapper;

    public SectorFundFlowDailySnapshotBasicServiceImpl(SectorFundFlowDailySnapshotMapper mapper,
                                                       SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public LocalDate findLatestTradeDate(String scope, LocalDate maxTradeDate) {
        return mapper.findLatestTradeDate(scope, maxTradeDate);
    }

    @Override
    public List<SectorFundFlowDailySnapshotPO> listByTradeDate(String scope, LocalDate tradeDate, int limit) {
        return mapper.listByTradeDate(scope, tradeDate, limit);
    }

    @Override
    public List<SectorFundFlowDailySnapshotPO> listRecent(String scope, LocalDate maxTradeDate, int days, int top) {
        return mapper.listRecent(scope, maxTradeDate, days, top);
    }
}
