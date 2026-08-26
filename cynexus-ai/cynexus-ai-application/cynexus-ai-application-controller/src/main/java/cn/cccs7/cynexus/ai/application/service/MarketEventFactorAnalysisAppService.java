package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.MarketEventFactorAnalysisDTO;
import cn.cccs7.cynexus.ai.api.request.MarketEventFactorAnalysisRequest;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorAnalysisDomainService;
import org.springframework.stereotype.Service;

@Service
public class MarketEventFactorAnalysisAppService {
    private final MarketEventFactorAnalysisDomainService domainService;

    public MarketEventFactorAnalysisAppService(MarketEventFactorAnalysisDomainService domainService) {
        this.domainService = domainService;
    }

    public MarketEventFactorAnalysisDTO.AnalysisResponse analyze(MarketEventFactorAnalysisRequest request) {
        MarketEventFactorAnalysisDomainService.AnalysisResult result = domainService.analyze(
                new MarketEventFactorAnalysisDomainService.AnalysisCommand(request == null ? null : request.getTradeDate(),
                        request == null ? null : request.getCohort(), request == null ? null : request.getMinLargeMovePct(),
                        request == null ? null : request.getMaxEvents(), request == null ? null : request.getSymbol()));
        return MarketEventFactorAnalysisDTO.AnalysisResponse.builder().ok(true).tradeDate(result.tradeDate()).cohort(result.cohort())
                .minLargeMovePct(result.minLargeMovePct()).source(result.source()).populationCount(result.populationCount()).sampledCount(result.sampledCount())
                .dataQuality(result.dataQuality()).warnings(result.warnings()).evidenceRefs(result.evidenceRefs()).analysisResultJson(result.analysis().toString())
                .providerCode(result.providerCode()).modelCode(result.modelCode()).promptVersion(result.promptVersion()).build();
    }
}
