package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.FuturesPositionAiDTO;
import cn.cccs7.cynexus.ai.api.request.FuturesPositionAiRequest;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import org.springframework.stereotype.Service;

@Service
public class FuturesPositionAiAppService {

    private final FuturesPositionAiDomainService domainService;

    public FuturesPositionAiAppService(FuturesPositionAiDomainService domainService) {
        this.domainService = domainService;
    }

    public FuturesPositionAiDTO.AnalysisResponse analyze(FuturesPositionAiRequest.AnalyzeRequest request) {
        FuturesPositionAiDomainService.AnalysisResult result = domainService.analyze(
                new FuturesPositionAiDomainService.AnalysisCommand(
                        request == null ? null : request.getVarietyCode(),
                        request == null ? null : request.getInstrumentCode(),
                        request == null ? null : request.getInstrumentName(),
                        request == null ? null : request.getExchangeCode(),
                        request == null ? null : request.getTradeDate(),
                        request == null ? null : request.getTop(),
                        request == null ? null : request.getCurrent(),
                        request == null ? null : request.getHistory(),
                        request == null ? null : request.getMembers()));
        return FuturesPositionAiDTO.AnalysisResponse.builder().ok(true)
                .varietyCode(result.varietyCode()).instrumentCode(result.instrumentCode()).tradeDate(result.tradeDate())
                .analysisResultJson(result.analysis().toString()).providerCode(result.providerCode())
                .modelCode(result.modelCode()).promptVersion(result.promptVersion()).build();
    }
}
