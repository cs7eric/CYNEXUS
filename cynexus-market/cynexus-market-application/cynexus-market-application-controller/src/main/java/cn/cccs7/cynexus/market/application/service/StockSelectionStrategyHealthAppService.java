package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyHealthBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyHealthDomainService;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionStrategyHealthAppService {

    private final StockSelectionStrategyHealthDomainService domainService;

    public StockSelectionStrategyHealthAppService(StockSelectionStrategyHealthDomainService domainService) {
        this.domainService = domainService;
    }

    public StockSelectionStrategyDTO.HealthResponse assess(StockSelectionStrategyRunBO.Scope scope,
                                                            String strategyCode,
                                                            String tradeDate) {
        StockSelectionStrategyHealthBO.Assessment assessment = domainService.assess(scope, strategyCode, tradeDate);
        return StockSelectionStrategyDTO.HealthResponse.builder()
                .strategyCode(assessment.strategyCode())
                .strategyVersionNo(assessment.strategyVersionNo())
                .requestedTradeDate(toText(assessment.requestedTradeDate()))
                .actualTradeDate(toText(assessment.actualTradeDate()))
                .status(assessment.status())
                .reason(assessment.reason())
                .recommendedAction(assessment.recommendedAction())
                .universeCount(assessment.universeCount())
                .candidateCount(assessment.candidateCount())
                .signalDensityPct(assessment.signalDensityPct())
                .freshnessLagDays(assessment.freshnessLagDays())
                .recentPerformance(performance(assessment.recentPerformance()))
                .source(assessment.source())
                .checks(assessment.checks().stream().map(this::check).toList())
                .warnings(assessment.warnings())
                .build();
    }

    private StockSelectionStrategyDTO.HealthCheck check(StockSelectionStrategyHealthBO.Check value) {
        return StockSelectionStrategyDTO.HealthCheck.builder()
                .code(value.code()).label(value.label()).status(value.status()).actualValue(value.actualValue())
                .threshold(value.threshold()).detail(value.detail()).build();
    }

    private StockSelectionStrategyDTO.RecentPerformance performance(StockSelectionStrategyHealthBO.RecentPerformance value) {
        return value == null ? null : StockSelectionStrategyDTO.RecentPerformance.builder()
                .evaluatedTradeCount(value.evaluatedTradeCount()).winRatePct(value.winRatePct())
                .averageExcessReturnPct(value.averageExcessReturnPct()).asOfDate(toText(value.asOfDate())).build();
    }

    private String toText(java.time.LocalDate value) {
        return value == null ? null : value.toString();
    }
}
