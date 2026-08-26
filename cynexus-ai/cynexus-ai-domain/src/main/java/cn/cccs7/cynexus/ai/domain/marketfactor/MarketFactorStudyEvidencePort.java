package cn.cccs7.cynexus.ai.domain.marketfactor;

import java.util.List;
import java.util.Map;

/** Owner-scoped, persisted study evidence. Raw sequences and live market reads are intentionally excluded. */
public interface MarketFactorStudyEvidencePort {
    EvidenceSnapshot load(String studyId, String principal, String tenant, String signature);

    record EvidenceSnapshot(String studyId, String status, String dataCutoffAt, String leakageCheckStatus,
                            List<String> warnings, Map<String, String> evidenceRefs, Map<String, Object> modelInput) { }
}
