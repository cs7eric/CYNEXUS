package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.FuturesPositionDTO;
import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;
import cn.cccs7.cynexus.market.domain.service.futures.FuturesPositionQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FuturesPositionQueryAppService {
    private final FuturesPositionQueryDomainService domainService;

    public FuturesPositionQueryAppService(FuturesPositionQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public FuturesPositionDTO.Latest latest(String tradeDate, String scopeType, String exchangeCode,
                                            String varietyCode, int top, String sort, String sortDir,
                                            Integer page, Integer pageSize) {
        return latestDto(domainService.latest(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize));
    }

    public FuturesPositionDTO.Latest instruments(String tradeDate, String scopeType, String exchangeCode,
                                                 String varietyCode, int top, String sort, String sortDir,
                                                 Integer page, Integer pageSize) {
        return latestDto(domainService.instruments(tradeDate, scopeType, exchangeCode, varietyCode, top, sort, sortDir, page, pageSize));
    }

    public List<FuturesPositionDTO.Member> members(String tradeDate, String scopeType, String exchangeCode,
                                                   String instrumentCode, String rankType, int top) {
        return domainService.members(tradeDate, scopeType, exchangeCode, instrumentCode, rankType, top)
                .stream().map(this::member).toList();
    }

    public List<FuturesPositionDTO.History> history(String tradeDate, String scopeType, String exchangeCode,
                                                     String instrumentCode, int top, int limit) {
        return domainService.history(tradeDate, scopeType, exchangeCode, instrumentCode, top, limit)
                .stream().map(this::history).toList();
    }

    public FuturesPositionDTO.Coverage coverage(String tradeDate) {
        return coverage(domainService.coverage(tradeDate));
    }

    private FuturesPositionDTO.Latest latestDto(FuturesPositionQueryBO.Latest b) {
        return FuturesPositionDTO.Latest.builder().ok(b.ok()).error(b.error())
                .requestedTradeDate(b.requestedTradeDate()).tradeDate(b.tradeDate())
                .fallbackTradeDate(b.fallbackTradeDate()).scopeType(b.scopeType())
                .exchangeCode(b.exchangeCode()).varietyCode(b.varietyCode()).top(b.top())
                .page(b.page().number()).pageSize(b.page().size()).count(b.count())
                .instruments(b.instruments().stream().map(this::instrument).toList())
                .aggregate(aggregate(b.aggregate())).coverage(coverage(b.coverage())).warnings(b.warnings()).build();
    }

    private FuturesPositionDTO.Instrument instrument(FuturesPositionQueryBO.Instrument b) {
        return FuturesPositionDTO.Instrument.builder().exchangeCode(b.exchangeCode()).instrumentCode(b.instrumentCode()).varietyCode(b.varietyCode())
                .contractCode(b.contractCode()).volume(b.volume()).longPosition(b.longPosition())
                .shortPosition(b.shortPosition()).netPosition(b.netPosition()).longShortRatio(b.longShortRatio())
                .longChange(b.longChange()).shortChange(b.shortChange()).source(b.source()).build();
    }

    private FuturesPositionDTO.Aggregate aggregate(FuturesPositionQueryBO.Aggregate b) {
        if (b == null) return null;
        return FuturesPositionDTO.Aggregate.builder().volume(b.volume()).longPosition(b.longPosition())
                .shortPosition(b.shortPosition()).netPosition(b.netPosition()).longShortRatio(b.longShortRatio())
                .longChange(b.longChange()).shortChange(b.shortChange()).build();
    }

    private FuturesPositionDTO.Member member(FuturesPositionQueryBO.Member b) {
        return FuturesPositionDTO.Member.builder().tradeDate(b.tradeDate()).exchangeCode(b.exchangeCode())
                .scopeType(b.scopeType()).instrumentCode(b.instrumentCode()).varietyCode(b.varietyCode())
                .contractCode(b.contractCode()).rankType(b.rankType()).rankNo(b.rankNo()).memberName(b.memberName())
                .rankValue(b.rankValue()).rankChange(b.rankChange()).source(b.source()).build();
    }

    private FuturesPositionDTO.History history(FuturesPositionQueryBO.History b) {
        return FuturesPositionDTO.History.builder().tradeDate(b.tradeDate()).instrumentCode(b.instrumentCode())
                .varietyCode(b.varietyCode()).volume(b.volume()).longPosition(b.longPosition())
                .shortPosition(b.shortPosition()).netPosition(b.netPosition()).longShortRatio(b.longShortRatio())
                .longChange(b.longChange()).shortChange(b.shortChange()).source(b.source()).build();
    }

    private FuturesPositionDTO.Coverage coverage(FuturesPositionQueryBO.Coverage b) {
        if (b == null) return null;
        return FuturesPositionDTO.Coverage.builder().tradeDate(b.tradeDate())
                .items(b.items().stream().map(item -> FuturesPositionDTO.CoverageItem.builder()
                        .tradeDate(item.tradeDate()).exchangeCode(item.exchangeCode()).provider(item.provider())
                        .status(item.status()).selected(item.selected()).expectedInstrumentCount(item.expectedInstrumentCount())
                        .actualInstrumentCount(item.actualInstrumentCount()).rowCount(item.rowCount())
                        .completedAt(item.completedAt()).build()).toList()).build();
    }
}
