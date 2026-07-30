package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionAggregatePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionCoveragePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionInstrumentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionMemberPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.FuturesPositionQueryMapper;
import cn.cccs7.cynexus.market.infra.basic.service.FuturesPositionQueryBasicService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FuturesPositionQueryBasicServiceImpl implements FuturesPositionQueryBasicService {
    private final FuturesPositionQueryMapper mapper;

    public FuturesPositionQueryBasicServiceImpl(FuturesPositionQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) { return Optional.ofNullable(mapper.findLatestTradeDate(endDate)); }
    @Override public List<FuturesPositionInstrumentPO> listInstruments(LocalDate d, String s, String e, String v, int t, String sort, String dir, int o, int l) { return mapper.listInstruments(d, s, e, v, t, sort, dir, o, l); }
    @Override public int countInstruments(LocalDate d, String s, String e, String v) { return mapper.countInstruments(d, s, e, v); }
    @Override public FuturesPositionAggregatePO aggregate(LocalDate d, String s, String e, String v, int t) { return mapper.aggregate(d, s, e, v, t); }
    @Override public List<FuturesPositionMemberPO> listMembers(LocalDate d, String s, String e, String i, String r, int t) { return mapper.listMembers(d, s, e, i, r, t); }
    @Override public List<FuturesPositionHistoryPO> listHistory(String s, String e, String i, int t, int l) { return mapper.listHistory(s, e, i, t, l); }
    @Override public List<FuturesPositionCoveragePO> listCoverage(LocalDate d) { return mapper.listCoverage(d); }
}
