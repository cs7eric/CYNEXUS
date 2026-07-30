package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;
import cn.cccs7.cynexus.market.domain.repository.FuturesPositionRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionAggregatePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionCoveragePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionInstrumentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionMemberPO;
import cn.cccs7.cynexus.market.infra.basic.service.FuturesPositionQueryBasicService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FuturesPositionRepositoryImpl implements FuturesPositionRepository {
    private final FuturesPositionQueryBasicService service;

    public FuturesPositionRepositoryImpl(FuturesPositionQueryBasicService service) {
        this.service = service;
    }

    @Override
    public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
        return service.findLatestTradeDate(endDate);
    }

    @Override
    public List<FuturesPositionQueryBO.Instrument> listInstruments(LocalDate tradeDate, String scopeType,
                                                                    String exchangeCode, String varietyCode,
                                                                    int top, String sort, String sortDir,
                                                                    int offset, int limit) {
        return service.listInstruments(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, offset, limit)
                .stream().map(this::instrument).toList();
    }

    @Override
    public int countInstruments(LocalDate tradeDate, String scopeType, String exchangeCode, String varietyCode) {
        return service.countInstruments(tradeDate, scopeType, exchangeCode, varietyCode);
    }

    @Override
    public FuturesPositionQueryBO.Aggregate aggregate(LocalDate tradeDate, String scopeType,
                                                       String exchangeCode, String varietyCode, int top) {
        FuturesPositionAggregatePO value = service.aggregate(tradeDate, scopeType, exchangeCode, varietyCode, top);
        return value == null ? null : new FuturesPositionQueryBO.Aggregate(value.getVolume(), value.getLongPosition(),
                value.getShortPosition(), value.getNetPosition(), value.getLongShortRatio(),
                value.getLongChange(), value.getShortChange());
    }

    @Override
    public List<FuturesPositionQueryBO.Member> listMembers(LocalDate tradeDate, String scopeType,
                                                            String exchangeCode, String instrumentCode,
                                                            String rankType, int top) {
        return service.listMembers(tradeDate, scopeType, exchangeCode, instrumentCode, rankType, top)
                .stream().map(this::member).toList();
    }

    @Override
    public List<FuturesPositionQueryBO.History> listHistory(String scopeType, String exchangeCode,
                                                             String instrumentCode, int top, int limit) {
        return service.listHistory(scopeType, exchangeCode, instrumentCode, top, limit)
                .stream().map(this::history).toList();
    }

    @Override
    public List<FuturesPositionQueryBO.CoverageItem> listCoverage(LocalDate tradeDate) {
        return service.listCoverage(tradeDate).stream().map(this::coverage).toList();
    }

    private FuturesPositionQueryBO.Instrument instrument(FuturesPositionInstrumentPO value) {
        return new FuturesPositionQueryBO.Instrument(value.getInstrumentCode(), value.getVarietyCode(),
                value.getContractCode(), value.getVolume(), value.getLongPosition(), value.getShortPosition(),
                value.getNetPosition(), value.getLongShortRatio(), value.getLongChange(), value.getShortChange(),
                value.getSource());
    }

    private FuturesPositionQueryBO.Member member(FuturesPositionMemberPO value) {
        return new FuturesPositionQueryBO.Member(value.getTradeDate(), value.getExchangeCode(), value.getScopeType(),
                value.getInstrumentCode(), value.getVarietyCode(), value.getContractCode(), value.getRankType(),
                value.getRankNo(), value.getMemberName(), value.getRankValue(), value.getRankChange(), value.getSource());
    }

    private FuturesPositionQueryBO.History history(FuturesPositionHistoryPO value) {
        return new FuturesPositionQueryBO.History(value.getTradeDate(), value.getInstrumentCode(), value.getVarietyCode(),
                value.getVolume(), value.getLongPosition(), value.getShortPosition(), value.getNetPosition(),
                value.getLongShortRatio(), value.getLongChange(), value.getShortChange(), value.getSource());
    }

    private FuturesPositionQueryBO.CoverageItem coverage(FuturesPositionCoveragePO value) {
        return new FuturesPositionQueryBO.CoverageItem(value.getTradeDate(), value.getExchangeCode(), value.getProvider(),
                value.getStatus(), Boolean.TRUE.equals(value.getSelected()),
                value.getExpectedInstrumentCount() == null ? 0 : value.getExpectedInstrumentCount(),
                value.getActualInstrumentCount() == null ? 0 : value.getActualInstrumentCount(),
                value.getRowCount() == null ? 0 : value.getRowCount(), value.getCompletedAt());
    }
}
