package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.MarketFactorStudySynthesisDTO;
import cn.cccs7.cynexus.ai.api.request.MarketFactorStudySynthesisRequest;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketFactorStudySynthesisDomainService;
import org.springframework.stereotype.Service;

@Service
public class MarketFactorStudySynthesisAppService {
    private final MarketFactorStudySynthesisDomainService domain;
    public MarketFactorStudySynthesisAppService(MarketFactorStudySynthesisDomainService domain) { this.domain = domain; }
    public MarketFactorStudySynthesisDTO.Response synthesize(MarketFactorStudySynthesisRequest request, String principal, String tenant, String signature) {
        var result = domain.synthesize(new MarketFactorStudySynthesisDomainService.Command(request == null ? null : request.getStudyId(), principal, tenant, signature));
        return MarketFactorStudySynthesisDTO.Response.builder().ok(true).studyId(result.studyId()).studyStatus(result.studyStatus()).dataCutoffAt(result.dataCutoffAt()).leakageCheckStatus(result.leakageCheckStatus()).warnings(result.warnings()).evidenceRefs(result.evidenceRefs()).synthesisResultJson(result.synthesis().toString()).providerCode(result.providerCode()).modelCode(result.modelCode()).promptVersion(result.promptVersion()).build();
    }
}
