package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunDomainService;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionStrategyRunAppService {

    private final StockSelectionStrategyRunDomainService domainService;

    public StockSelectionStrategyRunAppService(StockSelectionStrategyRunDomainService domainService) {
        this.domainService = domainService;
    }

    public StockSelectionStrategyDTO.RunResponse run(StockSelectionStrategyRunBO.Scope scope, String strategyCode, String tradeDate) {
        StockSelectionStrategyRunBO.Run value = domainService.run(scope, strategyCode, tradeDate);
        return StockSelectionStrategyDTO.RunResponse.builder()
                .id(value.id()).strategyCode(value.strategyCode()).strategyVersionId(value.strategyVersionId()).strategyVersionNo(value.strategyVersionNo())
                .requestedTradeDate(date(value.requestedTradeDate())).actualTradeDate(date(value.actualTradeDate())).status(value.status())
                .universeCount(value.universeCount()).matchedCount(value.matchedCount()).source(value.source()).warnings(value.warnings())
                .candidates(value.candidates().stream().map(this::candidate).toList()).build();
    }

    private StockSelectionStrategyDTO.Candidate candidate(StockSelectionStrategyRunBO.Candidate value) {
        return StockSelectionStrategyDTO.Candidate.builder().id(value.id()).stockCode(value.stockCode()).stockName(value.stockName())
                .rank(value.rank()).score(value.score()).reasonSummary(value.reasonSummary()).snapshot(value.snapshot())
                .evidence(value.evidence().stream().map(item -> StockSelectionStrategyDTO.Evidence.builder()
                        .code(item.code()).label(item.label()).operator(item.operator()).expectedValue(item.expectedValue())
                        .actualValue(item.actualValue()).matched(item.matched()).build()).toList()).build();
    }

    private String date(java.time.LocalDate value) {
        return value == null ? null : value.toString();
    }
}
