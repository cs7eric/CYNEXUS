package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.StockSelectionAiDTO;
import cn.cccs7.cynexus.ai.api.request.StockSelectionAiRequest;
import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiDomainService;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionAiAppService {

    private final StockSelectionAiDomainService domainService;

    public StockSelectionAiAppService(StockSelectionAiDomainService domainService) {
        this.domainService = domainService;
    }

    public StockSelectionAiDTO.AnalysisResponse analyze(StockSelectionAiRequest.AnalyzeRequest request) {
        StockSelectionAiDomainService.AnalysisResult result = domainService.analyze(new StockSelectionAiDomainService.AnalysisCommand(
                request == null ? null : request.getResearchIntent(), request == null ? null : request.getMaxCandidates(),
                request == null ? null : request.getCriteria()));
        return StockSelectionAiDTO.AnalysisResponse.builder().ok(true).tradeDate(result.tradeDate()).source(result.source())
                .universeCount(result.universeCount()).candidateCount(result.candidateCount()).warnings(result.warnings())
                .analysisResultJson(result.analysis().toString()).providerCode(result.providerCode()).modelCode(result.modelCode())
                .promptVersion(result.promptVersion()).build();
    }

    public StockSelectionAiDTO.IntentPlanResponse parseIntent(StockSelectionAiRequest.IntentPlanRequest request) {
        StockSelectionAiDomainService.IntentPlan plan = domainService.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                request == null ? null : request.getResearchIntent(), request == null ? null : request.getCriteria()));
        return StockSelectionAiDTO.IntentPlanResponse.builder().normalizedIntent(plan.normalizedIntent()).criteria(plan.criteria())
                .proposedConditions(plan.proposedConditions().stream().map(condition -> StockSelectionAiDTO.ProposedCondition.builder()
                        .category(condition.category()).sourcePhrase(condition.sourcePhrase()).label(condition.label())
                        .definition(condition.definition()).executionStatus(condition.executionStatus())
                        .dataRequirements(condition.dataRequirements()).build()).toList())
                .appliedRules(plan.appliedRules()).assumptions(plan.assumptions()).unsupportedPhrases(plan.unsupportedPhrases())
                .warnings(plan.warnings()).providerCode(plan.providerCode()).modelCode(plan.modelCode())
                .promptVersion(plan.promptVersion()).build();
    }
}
