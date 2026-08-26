package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.marketfactor.MarketFactorStudyEvidencePort;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketFactorStudyDTO;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Converts owner-scoped immutable study results into bounded, reference-addressable model input. */
@Component
public class MarketFactorStudyEvidenceAdapter implements MarketFactorStudyEvidencePort {
    private final MarketResearchFeignClient client;
    public MarketFactorStudyEvidenceAdapter(MarketResearchFeignClient client) { this.client = client; }
    @Override public EvidenceSnapshot load(String studyId, String principal, String tenant, String signature) {
        Result<MarketFactorStudyDTO.Response> studyResult = client.getMarketFactorStudy(studyId, principal, tenant, signature);
        if (studyResult == null || studyResult.isFailed() || studyResult.getData() == null) throw new IllegalStateException("Market factor study is unavailable");
        Result<List<MarketFactorStudyDTO.FactorResultResponse>> discoveryResult = client.getMarketFactorStudyDiscoveries(studyId, 0, 50, principal, tenant, signature);
        if (discoveryResult == null || discoveryResult.isFailed() || discoveryResult.getData() == null) throw new IllegalStateException("Market factor discoveries are unavailable");
        MarketFactorStudyDTO.Response study = studyResult.getData(); LinkedHashMap<String, String> refs = new LinkedHashMap<>();
        for (MarketFactorStudyDTO.FactorResultResponse item : discoveryResult.getData()) {
            if (item == null || item.getDiscoveryCode() == null) continue;
            String prefix = "discovery:" + item.getDiscoveryCode();
            refs.put(prefix + ".recipe", "recipe=" + String.valueOf(item.getRecipe()));
            refs.put(prefix + ".statistics", "statistics=" + String.valueOf(item.getStatistics()));
            refs.put(prefix + ".validation", "validation=" + String.valueOf(item.getValidation()));
            refs.put(prefix + ".evidence", "evidence=" + String.valueOf(item.getEvidence()));
        }
        Map<String, Object> input = new LinkedHashMap<>(); input.put("study_id", study.getStudyId()); input.put("status", study.getStatus());
        input.put("data_cutoff_at", study.getDataCutoffAt()); input.put("feature_registry_version", study.getFeatureRegistryVersion());
        input.put("leakage_check_passed", study.getLeakageCheckPassed()); input.put("warnings", study.getWarnings() == null ? List.of() : study.getWarnings()); input.put("evidence_refs", refs);
        return new EvidenceSnapshot(study.getStudyId(), study.getStatus(), study.getDataCutoffAt(), Boolean.TRUE.equals(study.getLeakageCheckPassed()) ? "PASSED" : "FAILED", study.getWarnings() == null ? List.of() : study.getWarnings(), Map.copyOf(refs), Map.copyOf(input));
    }
}
